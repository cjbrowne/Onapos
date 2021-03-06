package onapos;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

public class CollectionFile {
	private File onDisk;
	private Collection collection;
	
	/**
	 * Constructor for reading collections from disk
	 * @param infile the file containing the collection
	 */
	public CollectionFile(File infile) {
		onDisk = infile;
	}
	
	/**
	 * Constructor for writing collections to disk
	 * @param c the collection to write
	 * @param outfile the file to write to
	 */
	public CollectionFile(Collection c,File outfile) {
		onDisk = outfile;
		collection = c;
	}

	/**
	 * Provides an interface for writing collections to disk directly
	 * @param c the collection to write
	 */
	public void write(Collection c) {
		collection = c;
		write();
	}
	
	/**
	 * writes this file's collection to disk
	 */
	public void write() {
		FileWriter writer;
		BufferedWriter buffedWriter;
		if(!onDisk.exists()) {
			try {
				onDisk.createNewFile();
			} catch (IOException e) {
				if(Onapos.DEBUG_MODE) {
					System.err.println("WARNING: collection file not saved (could not create file): "+onDisk.getAbsolutePath());
				}
				return;
			}
		}
		if(!onDisk.canWrite()) {
			if(Onapos.DEBUG_MODE) {
				System.err.println("WARNING: collection file not saved (could not open file for writing): "+onDisk.getAbsolutePath());
			}
			return;
		}
		if(onDisk.isDirectory()) {
			if(Onapos.DEBUG_MODE) {
				System.err.println("WARNING: collection file not saved (tried to save as directory)");
			}
			return;
		}
		try {
			writer = new FileWriter(onDisk);
			buffedWriter = new BufferedWriter(writer);
		} catch (IOException e) {
			if(Onapos.DEBUG_MODE) {
				System.err.println("WARNING: file deleted before we could write to it: "+onDisk.getAbsolutePath());
			}
			return;
		}
		try {
			buffedWriter.write("name:"+collection.getName());
			buffedWriter.newLine();
			buffedWriter.write("type:"+collection.getType());
			buffedWriter.newLine();
			Map<String,PropertyType> properties = collection.getProperties();
			for(Entry<String,PropertyType> entry : properties.entrySet()) {
				buffedWriter.write("field:");
				buffedWriter.write(entry.getKey());
				buffedWriter.append(",");
				buffedWriter.write(entry.getValue().toString());
				buffedWriter.newLine();
			}
			List<Item> items = collection.getItems();
			for(Item i : items) {
				buffedWriter.write("item {");
				buffedWriter.newLine();
				// write tags
				if(i.getTags().size() != 0) {
					buffedWriter.write("tags:");
					// keep a reference to the last tag, awbc
					String lastTag = i.getTags().get(i.getTags().size()-1);
					for(String tag : i.getTags()) {
						buffedWriter.write(tag);
						// use that reference to see if we're on the last tag
						if(tag == lastTag) break;
						// print a comma if not
						buffedWriter.write(',');
					}
				}
				for(Entry<String,Property> p : i.getProperties().entrySet()) {
					buffedWriter.write(p.getKey());
					buffedWriter.append(":");
					try {
						switch(p.getValue().getType()) {
							case STRING:
								buffedWriter.write(p.getValue().getString());
								break;
							case INTEGER:
								buffedWriter.write(new Integer(p.getValue().getInt()).toString());
								break;
							case DOUBLE:
								buffedWriter.write(new Double(p.getValue().getDouble()).toString());
								break;
							case DATE:
								buffedWriter.write(p.getValue().getDate().toString());
								break;
							case BOOLEAN:
								if(p.getValue().getBoolean())
									buffedWriter.write("yes");
								else
									buffedWriter.write("no");
								break;
							default:
								if(Onapos.DEBUG_MODE) {
									System.err.println("WARNING: saving naked property (may not get loaded): "+p.getKey());
								}
								break;
						}
						buffedWriter.newLine();
						buffedWriter.flush();
					} catch (PropertyException e) {
						// TODO: if PropertyException gets thrown any other way, this code becomes reachable
						if(Onapos.DEBUG_MODE) {
							System.err.println("Unreachable code, but the compiler don't care none anyhow!");
						}
						System.exit(42);
					}
				}
				buffedWriter.append("}");
				buffedWriter.newLine();
				buffedWriter.flush();
			}
			buffedWriter.close();
		} catch(IOException e) {
			if(Onapos.DEBUG_MODE) {
				System.err.println("WARNING: file may be corrupted (IOException encountered while writing)");
				e.printStackTrace(System.err);
			}
		}
	}
	
	/**
	 * Read a collection from disk
	 * @return the collection we read
	 */
	public Collection read() {
		FileReader reader;
		BufferedReader buffedReader;
		if(!onDisk.canRead()) {
			if(Onapos.DEBUG_MODE) {
				System.err.println("WARNING: collection file could not be read: "+onDisk.getName());
			}
			return null;
		}
		if(onDisk.isDirectory()) {
			if(Onapos.DEBUG_MODE) {
				System.err.println("WARNING: tried to load a directory as a collection!");
			}
			return null;
		}
		try {
			reader = new FileReader(onDisk);
			buffedReader = new BufferedReader(reader);
		} catch(FileNotFoundException e) {
			if(Onapos.DEBUG_MODE) {
				System.err.println("WARNING: file deleted before we could read it: "+onDisk.getName());
			}
			return null;
		}
		try {
			String curLine;
			String collectionName = "Untitled Collection";
			String collectionType = "Generic";
			Map<String,PropertyType> properties = new HashMap<String,PropertyType>();
			collection = new Collection(collectionName,collectionType);
			while((curLine = buffedReader.readLine()) != null) {
				if(curLine.trim().startsWith("name:")) {
					collectionName = curLine.substring(curLine.indexOf(':')+1);
					collection.setName(collectionName);
				}
				if(curLine.trim().startsWith("type:")) {
					collectionType = curLine.substring(curLine.indexOf(':')+1);
					collection.setType(collectionType);
				}
				if(curLine.trim().startsWith("field:")) {
					String propertyName = curLine.trim().substring(curLine.trim().indexOf(':')+1,curLine.trim().indexOf(','));
					String propertyType = curLine.trim().substring(curLine.trim().indexOf(',')+1);
					properties.put(propertyName, Property.getTypeByName(propertyType));
				}
				if(curLine.trim().startsWith("item")) {
					Item curItem = new Item(collection.generateUID());
					List<String> tags = new ArrayList<String>();
					boolean foundLastBracket = false;
					while(curLine != null && foundLastBracket == false) {
						curLine = buffedReader.readLine();
						if(curLine.contains("}") && (curLine.indexOf("}") == 0 || curLine.charAt(curLine.indexOf("}")-1)!='\\')) {
							foundLastBracket = true;
						}
						for(Entry<String,PropertyType> e : properties.entrySet()) {
							if(curLine.trim().startsWith("tags")) {
								StringTokenizer tok = new StringTokenizer(curLine.substring(curLine.indexOf(":")+1),",");
								while(tok.hasMoreTokens()) {
									tags.add(tok.nextToken());
								}
							}
							if(curLine.trim().startsWith(e.getKey())) {
								Property p;
								String propertyString = curLine.substring(curLine.indexOf(":")+1);
								switch(e.getValue()) {
								case STRING:
									p = new Property(e.getValue(),propertyString);
									break;
								case INTEGER:
									p = new Property(e.getValue(),Integer.parseInt(propertyString));
									break;
								case DATE:
									try {
										p = new Property(e.getValue(),Onapos.SDF.parse(propertyString));
									} catch (ParseException pe) {
										System.err.println("WARNING: Date could not be parsed, storing as String instead");
										p = new Property(e.getValue(),propertyString);
									}
									break;
								case DOUBLE:
									p = new Property(e.getValue(),Double.parseDouble(propertyString));
									break;
								case BOOLEAN:
									p = new Property(e.getValue(),Boolean.parseBoolean(propertyString));
									break;
								default:
									p = new Property(e.getValue(),propertyString);
									break;
								}
								curItem.addProperty(e.getKey(),p);
							}
							curItem.addTags(tags);
						}
					}
					collection.addItem(curItem);
				}
			}
			collection.addProperties(properties);
			return collection;
		} catch(IOException e) {
			if(Onapos.DEBUG_MODE) {
				System.err.println("WARNING: exception occurred while reading file:"+onDisk.getName());
				e.printStackTrace(System.err);
			}
			return null;
		}
	}
}
