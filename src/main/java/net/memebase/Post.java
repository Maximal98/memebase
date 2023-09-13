package net.memebase;

import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;

public class Post {
	public int id;
	public int poster;
	public int commentIndex;
	public String title;
	public String[] tags;
	public String imgHash;
	public ArrayList<Comment> comments;
	public Post( int newId, int newPoster, String newTitle, String[] newTags ) {
			id = newId;
			poster = newPoster;
			commentIndex = 0;
			title = newTitle;
			tags = newTags;
			imgHash = "0";
			comments = new ArrayList<>();
	}
	public Post() {
		// this is for the json parser for db loader
		comments = new ArrayList<>();
	}
}
