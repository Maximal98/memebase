package net.memebase;

public class Comment {
	public int id;
	public int post;
	public int poster;
	public String text;
	public Comment(int newId, int newPostId, int newPoster, String newText) {
		id = newId;
		post = newPostId;
		poster = newPoster;
		text = newText;
	}
}
