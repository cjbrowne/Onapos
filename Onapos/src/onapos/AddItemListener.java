package onapos;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JTextField;

public class AddItemListener implements ActionListener {

	private OnaposUI context;
	private Item item;
	private Map<JLabel,JTextField> properties;
	
	/**
	 * Constructor for the listener
	 * TODO: rearrange the code so this takes in a Map<String,Property>
	 * @param c the OnaposUI that created this listener ('context')
	 * @param ps the properties of the new item (JLabel,JTextField)
	 */
	public AddItemListener(OnaposUI c,Map<JLabel,JTextField> ps) {
		context = c;
		properties = ps;
	}
	
	/**
	 * Adds the item to the currently selected collection
	 * @param e unused
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		Collection col = context.getSelectedCollection();
		item = new Item(col.generateUID());
		for(Entry<JLabel,JTextField> entry : properties.entrySet()) {
			String propertyName = entry.getKey().getText();
			if(col.getProperties().containsKey(propertyName)) {
				PropertyType t = col.getProperties().get(propertyName);
				Property p = craftProperty(t,entry.getValue().getText());
				item.addProperty(propertyName, p);
			}
		}
		col.addItem(item);
		context.populateTable(col);
	}
	
	/**
	 * Helper function to create a Property from the PropertyType and String
	 * @param t the type of property to create
	 * @param v the String representation of the value of the new Property
	 * @return the newly-created Property
	 */
	private Property craftProperty(PropertyType t,String v) {
		switch(t) {
		case STRING:
			return new Property(t,v);
		case INTEGER:
			try {
				return new Property(t,Integer.parseInt(v));
			} catch (NumberFormatException e) {
				if(Onapos.DEBUG_MODE) {
					System.err.println("Warning: Could not parse integer "+v+", storing as 0 instead.");
				}
				return new Property(t,0);
			}
		case DOUBLE:
			try {
				return new Property(t,Double.parseDouble(v));
			} catch (NumberFormatException e) {
				if(Onapos.DEBUG_MODE) {
					System.err.println("Warning: Could not parse double "+v+", storing as 0.0 instead.");
				}
				return new Property(t,0.0);
			}
		case DATE:
			// allows us to keep a uniform date format across everywhere
			try {
				return new Property(t,Onapos.SDF.parse(v));
			} catch (ParseException e) {
				// continue as before, parsing as string (issue warning too)
				if(Onapos.DEBUG_MODE) {
					System.err.println("WARNING: date object unrecognised, parsing as string");
				}
				return new Property(t,v);
			}
		case BOOLEAN:
			if(v.toLowerCase().equals("yes") || v.toLowerCase().equals("true")) return new Property(t,true);
			return new Property(t,false);
		default:
			// parse it as string if all else fails
			return new Property(t,v);
		}
	}

}
