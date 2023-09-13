package net.memebase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;

@Produces( "application/json" )
@Consumes( "application/json" )
public class MemeDatabase {
	public int postIndex;
	public ArrayList<Post> posts;

	private final ObjectMapper mainMapper;
	private boolean locked;
	public MemeDatabase( int newPostIndex, ArrayList<Post> newPosts ) {
		postIndex = newPostIndex;
		posts = newPosts;
		mainMapper = new ObjectMapper();
		locked = false;
	}

	public MemeDatabase () {
		mainMapper = new ObjectMapper();
		locked = false;
	}
	@GET
	@Path( "/" )
	public Response GetServerStatus() {
		if( locked ) return Response.status( 500 ).build();
		return Response.ok().build();
	}

	@GET
	@Path( "/posts/" )
	public Response GetPostIndex() {
		if( locked ) return Response.status( 500 ).build();
		try { return Response.ok( mainMapper.writeValueAsString( new IndexResponse( this.postIndex ) ) ).build(); }
		catch ( JsonProcessingException exception ) {
			return Response.status( 500, "Internal server error." ).build();
		}
	}

	@GET
	@Path( "/posts/{id}/" )
	public Response GetPost( @PathParam("id") int id ) {
		if( locked ) return Response.status( 500 ).build();
		try { return Response.ok( mainMapper.writeValueAsString( posts.get( id ) ) ).build(); }
		catch ( JsonProcessingException exception ) { return Response.status( 400, "Bad JSON or unknown processing error" ).build(); }
	}

	@GET
	@Path( "/posts/{id}/comments" )
	public Response GetCommentIndex( @PathParam("id") int id ) {
		return Response.ok ( new IndexResponse( posts.get( id ).commentIndex ) ).build();
	}

	@GET
	@Path( "/posts/{postId}/comments/{commentId}" )
	public Response GetComment( @PathParam("postId") int postId, @PathParam("commentId") int commentId ) {
		try { return Response.ok( mainMapper.writeValueAsString( posts.get( postId ).comments.get( postId ) ) ).build(); }
		catch (JsonProcessingException exception) {return Response.status( 400, "Bad JSON or unknown processing error." ).build();}
	}

	@POST
	@Path( "/posts/" )
	public Response PublishPost( String submissionJson ) {
		if( locked ) return Response.status( 500 ).build();
		// TODO: if i implemented a macro ( eg "public Response ServerError() { return ...status(500).. } " ) would java optimize it away?
		// because it would be convenient to just be able to call "ResponsePresets.ServerError()" instead of this
		// if java didn't optimize it away, it would be causing unnecessary processing
		postIndex++;
		PostSubmission submission;
		try { submission = mainMapper.readValue( submissionJson, PostSubmission.class ); }
		catch ( JsonProcessingException exception ) {
			return Response.status( 400, "Bad JSON or unknown processing error." ).build();
		}
		posts.add( new Post( postIndex, 0, submission.title, submission.tags ) );
		return Response.status( 201, "Post published." ).build();
	}

	@POST
	@Path( "/posts/{id}/comments/" )
	public Response PostComment( @PathParam( "id" ) int id, String submissionJson ) {
		try {
			Post selectedPost = posts.get(id);
			if ( selectedPost == null ) { return Response.status( 400, "Unknown post or other error" ).build(); }
			CommentSubmission submission = mainMapper.readValue( submissionJson, CommentSubmission.class );
			selectedPost.commentIndex++;
			selectedPost.comments.add( new Comment( selectedPost.commentIndex, selectedPost.id, 0, submission.text ) );
			posts.set( id, selectedPost );
			return Response.ok().build();
		}
		catch (JsonProcessingException exception)
		{return Response.status(400, "Bad JSON or unknown processing error." ).build();}
	}

	public String Dump() {
		try { return mainMapper.writeValueAsString( this ); }
		catch( JsonProcessingException exception ) {
			exception.printStackTrace();
			System.out.println( "\n\n---------------------------------------------------" );
			System.out.println( "COULD NOT PARSE DATABASE. CHANGES CAN NOT BE SAVED." );
			System.out.println( "---------------------------------------------------\n\n" );
		}
		return "error!";
	}
	public void Lock() {
		locked = true;
	}
}
