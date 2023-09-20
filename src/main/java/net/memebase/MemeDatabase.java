package net.memebase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.memebase.Auth.AccountCreationData;
import net.memebase.Auth.AuthToken;
import net.memebase.Auth.LoginSubmission;
import net.memebase.Auth.User;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;

@Produces( "application/json" )
@Consumes( "application/json" )
public class MemeDatabase {
	public int postIndex;
	public ArrayList<Post> posts;
	public int userIndex;
	public ArrayList<User> users;

	private final ObjectMapper mainMapper;
	private boolean locked;
	private final Logger logger;
	public MemeDatabase () {
		mainMapper = new ObjectMapper();
		locked = false;
		logger = LoggerFactory.getLogger( Main.class );
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
		if( locked ) return Response.status( 500 ).build();
		return Response.ok ( new IndexResponse( posts.get( id ).commentIndex ) ).build();
	}

	@GET
	@Path( "/posts/{postId}/comments/{commentId}" )
	public Response GetComment( @PathParam("postId") int postId, @PathParam("commentId") int commentId ) {
		if( locked ) return Response.status( 500 ).build();
		try { return Response.ok( mainMapper.writeValueAsString( posts.get( postId ).comments.get( commentId ) ) ).build(); }
		catch (JsonProcessingException exception) {return Response.status( 400, "Bad JSON or unknown processing error." ).build();}
	}

	@POST
	@Path( "/posts/" )
	@Consumes( { MediaType.MULTIPART_FORM_DATA } )
	public Response PublishPost(
			@Multipart(value = "img",type="image/png") InputStream imageStream,
			@Multipart(value = "submission",type="application/json") String submissionJson
	) {
		if( locked ) return Response.status( 500 ).build();
		// TODO: if i implemented a macro ( eg "public Response ServerError() { return ...status(500).. } " ) would java optimize it away?
		// because it would be convenient to just be able to call "ResponsePresets.ServerError()" instead of this
		// if java didn't optimize it away, it would be causing unnecessary processing

		//  16777216 (16MiB) for video data
		// if( image.length > 8388608 ) {
		//	return Response.status( 400, "Image data too large." ).build();
		// }
		// TODO: image logic does not exist
		PostSubmission submission;
		try { submission = mainMapper.readValue( submissionJson, PostSubmission.class ); }
		catch ( JsonProcessingException exception ) {
			return Response.status( 400, "Bad JSON or unknown processing error." ).build();
		}
		postIndex++;
		posts.add( new Post( postIndex, 0, submission.title, submission.tags ) );
		logger.info( "Created post {}", postIndex );
		return Response.status( 201, "Post published." ).build();
	}

	@POST
	@Path( "/posts/{id}/comments/" )
	public Response PostComment( @PathParam( "id" ) int id, String submissionJson ) {
		if( locked ) return Response.status( 500 ).build();
		try {
			Post selectedPost = posts.get(id);
			if ( selectedPost == null ) { return Response.status( 400, "Unknown post or other error" ).build(); }
			CommentSubmission submission = mainMapper.readValue( submissionJson, CommentSubmission.class );
			selectedPost.commentIndex++;
			selectedPost.comments.add( new Comment( selectedPost.commentIndex, selectedPost.id, 0, submission.text ) );
			posts.set( id, selectedPost );
			logger.info( "Created comment number {} for Post {}.", selectedPost.commentIndex, postIndex);
			return Response.ok().build();
		}
		catch (JsonProcessingException exception)
		{return Response.status(400, "Bad JSON or unknown processing error." ).build();}
	}

	// TODO: Login
	@POST
	@Path( "/users/{id}/" )
	public Response CreateAccount( @PathParam( "id" ) int id, String jsonCreationData ) {
		try {
			AccountCreationData creationData = mainMapper.readValue(jsonCreationData, AccountCreationData.class);
			User newUser = new User();
			userIndex++;
			newUser.id = userIndex;
			newUser.name = creationData.name;
			MessageDigest mainDigest = MessageDigest.getInstance( "SHA3-384" );
			SecureRandom random = new SecureRandom();
			byte[] salt = new byte[8];
			random.nextBytes( salt );
			mainDigest.update( salt );

			byte[] bytes = mainDigest.digest(creationData.password.getBytes(StandardCharsets.UTF_8));
			StringBuilder passwordBuilder = new StringBuilder();
			for (byte aByte : bytes) {
				passwordBuilder.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
			}

			newUser.hashedPassword = passwordBuilder.toString();
			newUser.salt = salt;
			users.add( userIndex, newUser );
		}
		catch ( JsonProcessingException exception )
		{ return Response.status(400, "Bad JSON or unknown processing error." ).build();}
		catch ( NoSuchAlgorithmException exception )
		{ return Response.status( 500, "Unknown hashing algorithm" ).build(); }
		return Response.ok().build();
	}

	@POST
	@Path( "/users/{id}/login/" )
	public Response LogIntoAccount( @PathParam( "id" ) int id, String jsonLoginData ) {
		try {
			LoginSubmission loginData = mainMapper.readValue( jsonLoginData, LoginSubmission.class );
			User user = users.get( id );
			MessageDigest mainDigest = MessageDigest.getInstance( "SHA3-384" );
			mainDigest.update( user.salt );

			byte[] bytes = mainDigest.digest( loginData.password.getBytes(StandardCharsets.UTF_8) );
			StringBuilder passwordBuilder = new StringBuilder();
			for (byte aByte : bytes) {
				passwordBuilder.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
			}

			if( user.hashedPassword.contentEquals( passwordBuilder ) ) { //TODO: this returns false always says my IDE
				AuthToken token = new AuthToken();
				token.associatedUser = id;
				token.genUnique();
				user.validTokens.add( token );
				return Response.ok( mainMapper.writeValueAsString( token ) ).build();
			} else {
				return Response.status( 401, "Invalid password." ).build();
			}
		}
		catch ( JsonProcessingException exception )
		{ return Response.status( 400, "Bad JSON or unknown processing error." ).build(); }
		catch ( NoSuchAlgorithmException exception )
		{ return Response.status( 500, "Unknown hashing algorithm" ).build(); }
	}

	public String Dump() {
		try { return mainMapper.writeValueAsString( this ); }
		catch( JsonProcessingException exception ) {
			exception.printStackTrace();
			System.out.println("""
					
					---------------------------------------------------
					COULD NOT PARSE DATABASE. CHANGES CAN NOT BE SAVED.
					---------------------------------------------------
					
					""" );
		}
		return "error!";
	}
	public void Lock() {
		locked = true;
	}
}
