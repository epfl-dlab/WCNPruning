package ch.epfl.dlab.wikipedia.wcnpruning;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;

import ch.epfl.dlab.wikipedia.wcnpruning.Graph.Article;
import ch.epfl.dlab.wikipedia.wcnpruning.Graph.Category;

/**
 * 
 * @author Tiziano Piccardi tiziano.piccardi@epfl.ch
 *
 */
public class GraphPruner {

	Graph graph;
	double threshold;
	CategoryInfo[] categoriesInfo;
	Article[] articleByVirtualId;
	AtomicInteger processedCategories = new AtomicInteger(0);
	// BlockingQueue used by the consumer threads
	BlockingQueue<Category> readyCategories = new LinkedBlockingQueue<>();
	// The purity function used
	PurityScoreFunction scoreFunction;

	Timer timer = new Timer();
	OutputStreamWriter writer = null;
	BufferedWriter bw = null;

	int threads_number = 10;

	public void log(String message) {
		System.out.println(new Date() + " [" + threshold + "] " + message);
	}

	/**
	 * 
	 * @param graph
	 * @param scoreFunction
	 * @param threshold
	 */
	public GraphPruner(Graph graph, PurityScoreFunction scoreFunction, double threshold, int threads, String output_name) {

		this.threads_number = threads;

		// add logger feedback, every 20 seconds
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				log("Queue size: " + readyCategories.size() + " | Processed: " + processedCategories.get()
						+ " | Missing: " + (graph.categories.size() - processedCategories.get()));
			}
		}, 20 * 1000, 20 * 1000);

		// Initial assignments
		this.graph = graph;
		this.threshold = threshold;
		this.scoreFunction = scoreFunction;
		categoriesInfo = new CategoryInfo[graph.categories.size()];
		
		
		for (Category c : graph.categories.values())
			categoriesInfo[c.virtualId] = new CategoryInfo(c);

		articleByVirtualId = new Article[graph.articles.size()];
		for (Article a : graph.articles.values())
			articleByVirtualId[a.virtualId] = a;

		try {
			writer = new OutputStreamWriter(
					new FileOutputStream(new File(output_name)),
					StandardCharsets.UTF_8);
			bw = new BufferedWriter(writer);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Launch N threads
	 */
	public void run() {

		for (Category c : graph.categories.values())
			if (c.children.size() == 0)
				readyCategories.add(c);

		log("Start pruining...");

		ExecutorService executor = Executors.newFixedThreadPool(this.threads_number);
		for (int i = 0; i < this.threads_number; i++) {
			Runnable worker = new Runnable() {

				@Override
				public void run() {
					try {
						process();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
			executor.execute(worker);
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
		}

		timer.cancel();
		try {
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * The task of one thread. It take one category from the queue and processes it
	 * @throws InterruptedException
	 */
	public void process() throws InterruptedException {
		while (processedCategories.incrementAndGet() <= graph.categories.size()) {
			// this is a blocking and synchronized call
			// get the next from the queue
			Category category = readyCategories.take();

			// get the information wrapper
			CategoryInfo categoryInfo = categoriesInfo[category.virtualId];

			// get the immediate articles
			Set<Integer> articlesIds = category.getLocalArticles();
			// add all the IDs in the mailbox (sent from children)
			articlesIds.addAll(categoryInfo.getArticlesIds());

			// get Frequencies Type Distribution
			HashMap<String, Integer> ftd = new HashMap<>();
			for (int aid : articlesIds) {
				Article article = articleByVirtualId[aid];
				if (article.type != null) {
					Integer count = ftd.get(article.type);
					if (count == null)
						count = 0;
					ftd.put(article.type, count + 1);
				}
			}

			double score = scoreFunction.getScore(ftd);

			// Check for purity
			if (score > this.threshold) {
				categoriesInfo[category.virtualId].isPure = true;

				Map<String, Object> row = new HashMap<>();
				row.put("category", category.name);
				List<Integer> ids = new ArrayList<>();
				for (int vid : articlesIds)
					ids.add(articleByVirtualId[vid].id);
				row.put("articles", ids);
				row.put("score", score);
				writeRow(gson.toJson(row));
			} else {
				categoriesInfo[category.virtualId].isPure = false;
				// no propagation, delete article set
				articlesIds.clear();
			}

			// send message to all parents
			for (Category parent : category.parents) {
				CategoryInfo parentInfoWrapper = categoriesInfo[parent.virtualId];
				parentInfoWrapper.notify(category.virtualId, articlesIds, readyCategories);
			}

			// now all the parents are notified, and I can release the memory
			categoryInfo.releaseCache();

		}
	}

	public static Gson gson = new Gson();

	public synchronized void writeRow(String row) {

		try {
			bw.write(row);
			bw.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * It represents a "mail box" for the messaging logic. The visited categories
	 * send messages to the parent nodes.
	 * 
	 * @author Tiziano Piccardi tiziano.piccardi@epfl.ch
	 *
	 */
	public static class CategoryInfo {
		// The embedded category
		public final Category category;
		// If it is marked as pure
		public Boolean isPure;
		// The list of children categories that passed informations so far
		public Set<Integer> sendersIds = new HashSet<>();

		// The list of all the children of the category according to the pruning.
		// The list is compressed in a bit vector after the size reach a threshold
		// Explicit version:
		private Set<Integer> articlesIds = new HashSet<>();
		// Compressed version:
		private BitSet compressedArticles = null;

		// compression threshold
		int LIMIT_SIZE = (5000000 / 8) / 4;

		public CategoryInfo(Category category) {
			this.category = category;
		}

		/**
		 * destroy the mailbox
		 */
		public void releaseCache() {
			articlesIds = null;
			sendersIds = null;
			compressedArticles = null;
		}

		/**
		 * Get the list of all children
		 * @return
		 */
		public Set<Integer> getArticlesIds() {
			if (articlesIds != null)
				return articlesIds;
			else {
				Set<Integer> result = new HashSet<>();
				for (int i = 0; i < compressedArticles.length(); i++) {
					if (compressedArticles.get(i))
						result.add(i);
				}
				return result;
			}
		}

		/**
		 * Called by a category to notify the parents that it is pure and its articles
		 * must be propagated
		 * 
		 * @param virtualCategoryId
		 * @param childArticles
		 * @param queue
		 */
		public synchronized void notify(int virtualCategoryId, Set<Integer> childArticles,
				BlockingQueue<Category> queue) {

			// add the list of children
			sendersIds.add(virtualCategoryId);

			if (articlesIds != null) {
				articlesIds.addAll(childArticles);
				if (articlesIds.size() > LIMIT_SIZE) {
					compressedArticles = new BitSet();
					for (Integer aid : articlesIds)
						compressedArticles.set(aid);
					articlesIds = null;
				}
			} else {
				BitSet newSet = new BitSet();
				for (Integer aid : childArticles)
					newSet.set(aid);
				compressedArticles.or(newSet);
			}

			// Check if all the children categories are processed
			boolean canBeScheduled = true;
			for (Category child : category.children) {
				canBeScheduled &= sendersIds.contains(child.virtualId);
			}
			if (canBeScheduled)
				queue.add(category);

		}

		@Override
		public String toString() {
			return "CategoryInfo [category=" + category + ", isPure=" + isPure + ", sendersIds=" + sendersIds
					+ ", articlesIds=" + articlesIds + "]";
		}

	}

}
