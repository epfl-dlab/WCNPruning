package ch.epfl.dlab.wikipedia.wcnpruning;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @author Tiziano Piccardi tiziano.piccardi@epfl.ch
 *
 */
public class Graph {

	/**
	 * Index of the categories vertices
	 */
	public HashMap<String, Category> categories = new HashMap<String, Graph.Category>();
	/**
	 * Index of the articles vertices
	 */
	public HashMap<Integer, Article> articles = new HashMap<>();

	/**
	 * Get a reference to a category (with implicit creation)
	 * 
	 * @param name
	 * @return
	 */
	public synchronized Category getOrCreateCategory(String name) {
		Category result = categories.get(name);
		if (result == null) {
			result = new Category(name);
			categories.put(name, result);
		}
		return result;
	}

	/**
	 * Deep copy of the categories graph 
	 */
	public Graph clone() {
		Graph copy = new Graph();

		for (Category c : categories.values()) {
			Category category = copy.getOrCreateCategory(c.name);
			for (Category parent : c.parents)
				category.parents.add(copy.getOrCreateCategory(parent.name));
			for (Category child : c.children)
				category.children.add(copy.getOrCreateCategory(child.name));
			copy.categories.put(c.name, category);
		}

		for (Article a : articles.values()) {
			Article article = new Article();
			// article.virtualId = a.virtualId;
			article.id = a.id;
			article.type = a.type;
			article.title = a.title;
			for (Category c : a.categories) {
				article.categories.add(copy.getOrCreateCategory(c.name));
				copy.getOrCreateCategory(c.name).articles.add(article);
			}
			copy.articles.put(article.id, article);
		}

		return copy;

	}

	/**
	 * Assign/Reset the virtual IDs. This id has no gaps 
	 */
	public void packVirtualIds() {
		int categoryVirtualId = 0;
		for (Category c : categories.values()) {
			c.virtualId = categoryVirtualId;
			categoryVirtualId++;
		}
		int articleVirtualId = 0;
		for (Article a : articles.values()) {
			a.virtualId = articleVirtualId;
			articleVirtualId++;
		}
	}

	/**
	 * Copy the internal virtual IDs
	 * @param graph
	 */
	public void copyVirtualIds(Graph graph) {
		for (Category c : graph.categories.values()) {
			if(categories.containsKey(c.name)) {
				categories.get(c.name).virtualId = c.virtualId;
			}
		}
	}

	/**
	 * The definition of the category node 
	 * @author Tiziano Piccardi tiziano.piccardi@epfl.ch
	 *
	 */
	public static class Category {
		/**
		 * Implicit virtual id assigned to have an enumeration without gaps
		 */
		public int virtualId;
		/**
		 * Name/title of the category
		 */
		public String name;

		private Category(String name) {
			this.name = name;
		}

		public Set<Category> parents = new HashSet<>();
		public Set<Category> children = new HashSet<>();
		public Set<Article> articles = new HashSet<>();

		/**
		 * Get the articles of the category
		 * @return
		 */
		public Set<Integer> getLocalArticles() {
			Set<Integer> result = new HashSet<>();
			for (Article a : articles)
				result.add(a.virtualId);
			return result;
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Category)
				return ((Category) obj).name.equals(this.name);
			return false;
		}

		@Override
		public String toString() {
			return name;
		}

	}

	/**
	 * The definition of the article node
	 * @author Tiziano Piccardi tiziano.piccardi@epfl.ch
	 *
	 */
	public static class Article {
		/**
		 * Original article id (from Wikipedia database)
		 */
		public Integer id;
		/**
		 * Implicit virtual id assigned to have an enumeration without gaps
		 */
		public Integer virtualId;
		public Set<Category> categories = new HashSet<>();
		/**
		 * The type of the article
		 */
		public String type;
		/**
		 * The title of the article
		 */
		public String title;

		@Override
		public int hashCode() {
			return id.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Article)
				return ((Article) obj).id == this.id;
			return false;
		}

		@Override
		public String toString() {
			return "Article [id=" + id + ", virtualId=" + virtualId + ", categories=" + categories + ", type=" + type
					+ ", title=" + title + "]";
		}

	}

}
