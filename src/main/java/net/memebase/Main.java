package net.memebase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
	// TODO: json is a bad database format because we will be practically unable to write continuously
	// We would have to shift around huge amounts of data
	// lets say a database has 2000 posts.
	// if a user were to write a comment to post 3
	// All 1997 Posts after it would have to be shifted down
	// which would create horrible lag, which would over time desynchronise
	// the in memory db and the db on disk
	// or make the database only usable for ~10 users concurrently
	public static void main(String[] args) {
		File databaseFile = new File( "Database.json" );
		MemeDatabase MainDB = null;
		if( databaseFile.length() == 0 ) {
			System.out.println( "Database does not exist. creating it..." );
			MainDB = new MemeDatabase( -1, new ArrayList<>() );
		} else {
			System.out.println( "Database file exists, attempting to load it." );
			ObjectMapper loadMapper = new ObjectMapper();
			String databaseString = "";
			try {
				FileReader databaseReader = new FileReader( databaseFile );
				char[] characters = new char[(int) databaseFile.length()];
				databaseReader.read( characters );
				databaseReader.close();
				databaseString = new String( characters );
			}
			catch ( IOException exception ) {
				exception.printStackTrace();
				System.out.println( "\n\nException occurred while trying to load the database." );
				System.out.println( "Shutting down for safety reasons" );
				System.exit( 1 );
			}
			try { MainDB = loadMapper.readValue( databaseString, MemeDatabase.class ); }
			catch ( JsonProcessingException exception ) {
				exception.printStackTrace();
				System.out.println( "\n\nException occurred while parsing the database." );
				System.out.println( "Shutting down for safety reasons" );
				System.exit( 2 );
			}
		}
		if( MainDB == null ) {
			System.out.println( "Unknown database init error" );
			System.exit( 3 );
		}
		JAXRSServerFactoryBean serverFactory = new JAXRSServerFactoryBean();
		serverFactory.setResourceClasses( MemeDatabase.class );
		serverFactory.setResourceProvider( MemeDatabase.class, new SingletonResourceProvider( MainDB ) );
		serverFactory.setAddress( "http://localhost:8000/" );
		Server server = serverFactory.create();
		Scanner CLIScanner = new Scanner( System.in );
		boolean running = true;
		while( running ) {
			switch( CLIScanner.nextLine() ){
				// TODO: figure out what to do with the database CLI
				// TODO: accept remote connections through SSH tunneled plaintext or some shit.
				case "stop" -> {
					System.out.println("Shutting down server.");
					// TODO: Lock up before dumping DB
					// TODO: Continuous writing to disk
					try ( FileWriter databaseWriter = new FileWriter( databaseFile ) ) {
						String dumpString = MainDB.Dump();
						if( !( dumpString.equals( "error!" ) ) ) databaseWriter.write( dumpString );
						else { System.exit(4); }
					}
					catch ( IOException exception ) {
						exception.printStackTrace();
						System.out.println( "\n\n-------------------------------------" );
						System.out.println( "COULD NOT WRITE THE DATABASE TO DISK." );
						System.out.println( "-------------------------------------\n\n" );

						System.out.println( "The database write has failed." );
						System.out.println( "Please start a debugger on the server to view the contents of the database in memory" );
						System.out.println( "The program will now lock up to allow you more time to view its memory." );
						while ( true ) { //TODO: this causes a warn, is there any other way to lock up the JVM?
							try {
								Thread.currentThread().wait(1000000);
							} catch (InterruptedException exception1) {
								System.out.println("InterruptedException occurred.");
							}
						}
					}

					System.out.println( "Database written successfully." );
					server.stop();
					running = false;
					// TODO: server has strange behaviour where it has to be terminated when it stops because it hangs after writing the database
				}
				case "dump" -> System.out.println( MainDB.Dump() );
				default -> System.out.println("Unknown command.");
			}
		}
	}
}