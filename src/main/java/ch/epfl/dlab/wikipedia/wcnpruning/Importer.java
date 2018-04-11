package ch.epfl.dlab.wikipedia.wcnpruning;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.epfl.dlab.wikipedia.wcnpruning.Graph.Article;
import ch.epfl.dlab.wikipedia.wcnpruning.Graph.Category;

/**
 * 
 * @author Tiziano Piccardi tiziano.piccardi@epfl.ch
 *
 */
public class Importer {

	/**
	 * Load the edges of the graph. It creates the missing vertices
	 * @param edgesFile
	 * @return
	 */
	public static Graph loadGraph(String edgesFile) {
		Graph graph = new Graph();

		System.out.println("Loading categories...");
		
		try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(edgesFile), StandardCharsets.UTF_8))) {
			for (String line; (line = br.readLine()) != null;) {
				String[] edge = line.split("\t");
				String parent = edge[1];
				String child = edge[0];

				Category parentCategory = graph.getOrCreateCategory(parent);
				Category childCategory = graph.getOrCreateCategory(child);

				parentCategory.children.add(childCategory);
				childCategory.parents.add(parentCategory);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return graph;
	}
	

	/**
	 * Load the articles assigned to a category (one per row)
	 * @param graph
	 * @param articlesFile
	 * @return
	 */
	public static List<String> loadArticles(Graph graph, String articlesFile) {
		List<String> ignored = new ArrayList<>();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(
	                new FileInputStream(articlesFile), StandardCharsets.UTF_8))) {
			for (String line; (line = br.readLine()) != null;) {
				String[] info = line.split("\t");
				Integer aid = Integer.valueOf(info[0]);
				String title = info[1];
				String categoryName = info[2];

				Category category = graph.categories.get(categoryName);
				if (category != null) {

					Article article = graph.articles.get(aid);
					if (article == null) {
						article = new Article();
						article.id = aid;
						article.title = title;
						graph.articles.put(aid, article);
					}
					article.categories.add(category);
					category.articles.add(article);
				} else {
					ignored.add(categoryName);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ignored;
	}

	/**
	 * Load the types of an article. One per row
	 * @param graph
	 * @param typesFile
	 * @return
	 */
	public static long loadTypes(Graph graph, String typesFile) {
		long total = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(typesFile))) {
			for (String line; (line = br.readLine()) != null;) {
				String[] parts = line.split("\t");
				Integer aid = Integer.valueOf(parts[0]);
				String type = parts[1];

				Article article = graph.articles.get(aid);
				if (article != null) {
					article.type = type;
					total++;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return total;
	}

}
