package ch.epfl.dlab.wikipedia.wcnpruning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {
	public static void main(String[] args) {
		
		if (args.length < 3) {
			System.out.println("Missing parameters: edges_file category_articles articles_type");
			System.exit(0);
		}
		
		System.out.println("Pruning started");
		System.out.println("Loading...");
		Graph graph = Importer.loadGraph(args[0]);
		System.out.println("Total categories: " + graph.categories.size());
		System.out.println("Loading Articles...");
		Importer.loadArticles(graph, args[1]);
		System.out.println("Total articles: " + graph.articles.size());
		System.out.println("Loading types...");
		long count = Importer.loadTypes(graph, args[2]);
		System.out.println("Assigned " + count + " types...");


		graph.packVirtualIds();
		
		
		GraphPruner gpGini = new GraphPruner(graph, giniScoreFunction, 0.966, 40, giniScoreFunction.getName() + "_articles_scores.json");
		gpGini.run();
	}
	
	/**
	 * Dummy example of constant score
	 */
	static PurityScoreFunction constant1Function = new PurityScoreFunction() {

		@Override
		public double getScore(Map<String, Integer> ftd) {
			return 1;
		}

		@Override
		public String getName() {
			return "accept_all";
		}
		
	};

	/**
	 * Gini impurity function
	 */
	static PurityScoreFunction giniImpurityFunction = new PurityScoreFunction() {

		@Override
		public double getScore(Map<String, Integer> ftd) {
			double score = 0;
			int sum = ftd.values().stream().mapToInt(Integer::intValue).sum();
			for (Integer i : ftd.values()) {
				score += Math.pow(i.doubleValue()/sum, 2);
			}

			return score;
		}

		@Override
		public String getName() {
			return "ginipurity";
		}

	};

	/**
	 * Difference between the 2 peaks
	 */
	static PurityScoreFunction m1m2ScoreFunction = new PurityScoreFunction() {
		@Override
		public double getScore(Map<String, Integer> ftd) {
			if (ftd.size() < 2)
				return 1;

			Collection<Integer> counts = ftd.values();
			Iterator<Integer> it = counts.iterator();
			Integer max1 = -1;
			Integer max2 = -1;
			double total = 0l;
			while (it.hasNext()) {
				Integer c = it.next();
				total += c;
				if (c >= max1) {
					max2 = max1;
					max1 = c;
				} else if (c >= max2)
					max2 = c;
			}
			return (max1 - max2) / total;
		}

		@Override
		public String getName() {
			return "m1m2diff";
		}
	};

	/**
	 * Gini coefficient score
	 */
	static PurityScoreFunction giniScoreFunction = new PurityScoreFunction() {
		@Override
		public double getScore(Map<String, Integer> ftd) {
			if (ftd.size() < 2)
				return 1;

			List<Integer> values = new ArrayList<>();
			for (Integer c : ftd.values())
				values.add(c);
			for (int i = values.size(); i < 55; i++)
				values.add(0);

			Collections.sort(values, Collections.reverseOrder());

			double height = 0;
			double area = 0;
			for (Integer v : values) {
				height += v;
				area += height - v / 2d;
			}
			double fairArea = height * values.size() / 2d;
			return Math.abs((fairArea - area) / fairArea);

		}

		@Override
		public String getName() {
			return "gini";
		}
	};
}
