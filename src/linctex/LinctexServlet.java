package linctex;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.wave.api.*;
import com.google.wave.api.event.*;

public class LinctexServlet extends AbstractRobot {
	
	enum Mode {
		SYNTAX,
		ANNOTATION;
	}
	
	private static final long serialVersionUID = 1L;

	@Override
	protected String getRobotName() {
		return "LincTex";
	}
	
	@Override
	protected String getRobotAvatarUrl() {
		return "http://linc-tex.appspot.com/logo.png";
	}
	
	@Override
    public String getRobotProfilePageUrl() {
    	return "http://linc-tex.appspot.com/";
    }
	
	@Override
	public void onWaveletSelfAdded(WaveletSelfAddedEvent event) {
		introduce(event);
	}
	
	@Capability(contexts = {Context.SELF})
	@Override
	public void onDocumentChanged(DocumentChangedEvent event) {
		Mode mode = getMode(event);
		Blip blip = event.getBlip();
		
		if (event.getModifiedBy().startsWith("spelly") || event.getModifiedBy().startsWith("panda")) {
			return;
		}
		
		for (Integer i : blip.getElements().keySet() ) {
			Element e = blip.getElements().get(i);
			
			if ( e!= null && e.isImage()) {
				String url = e.getProperty("url");
				String formula = null;
				if ( url.contains("http://www.forkosh.dreamhost.com/mathtex.cgi?")) {
					// old version
					formula = url.replaceFirst("http://www.forkosh.dreamhost.com/mathtex.cgi?", "").substring(1);
				} else {
					// new version
					formula = url.replaceFirst("http://www.forkosh.com/mathtex.cgi?", "").substring(1);
				}
				
				if ( url != null && url.startsWith("http://www.forkosh.")) {
					BlipContentRefs ref = blip.range(i, i+1);
					if (mode == Mode.SYNTAX) {
						ref.replace("$$"+formula+"$$");
					} else {
						ref = ref.replace(formula);
						ref = BlipContentRefs.range(blip, i, i+formula.length());
						ref.annotate("robot.linctex", "formula");
						ref.annotate("style/backgroundColor", "rgb(200,200,200)");
					}
				}
			}
		}
	}
	
	@Capability(contexts= {Context.SELF} , filter = "\\$\\$[^\\$\\$]*\\$\\$")
	@Override
	public void onBlipSubmitted(BlipSubmittedEvent event) {
		Blip blip = event.getBlip();
		Mode mode = getMode(event);
		String text = blip.getContent();
		
		if (text.toLowerCase().contains("linctex?")) {
			introduce(event);
		}
		
		if (mode==Mode.SYNTAX) {
		
	    	Pattern p = Pattern.compile("\\$\\$[^\\$\\$]*\\$\\$");
	    	Matcher m = p.matcher(text);
			
	    	while ( m.find() ) {
	    		String formula = text.substring(m.start()+2, m.end()-2);
	    		
	    	    
	    	    Image image = new Image();
	    	    image.setCaption(formula);
	    	    image.setUrl("http://www.forkosh.com/mathtex.cgi?" + formula);
	    	    
	    		blip.all("$$"+ formula +"$$" ).replace(image);
	    	    
	    	}
		}
    	
    	Annotations anns = event.getBlip().getAnnotations();
    	List<Annotation> li = anns.get("robot.linctex");
    	if (li==null) return;
    	
    	for (Annotation ann :  li ){
			Range range = ann.getRange();
			String formula = blip.getContent().substring(range.getStart(), range.getEnd());
			
			Image image = new Image();
	    	image.setCaption(formula);
	    	image.setUrl("http://www.forkosh.com/mathtex.cgi?" + formula);
			
			blip.range(range.getStart(), range.getEnd()).replace(image);
    	}
    	
    	
	}
	
	@Override
	public void onFormButtonClicked(FormButtonClickedEvent event) {
		DataDocuments data = event.getWavelet().getDataDocuments();
		
		if (event.getButtonName().equals("syntax")) {
			
			data.set("robot.linctex.mode", "syntax");
			
		} else if ( event.getButtonName().equals("anno") ) {
			
			data.set("robot.linctex.mode", "annotate");
			
		}
	}
	
	private Mode getMode(Event ev) {
		Mode mode = Mode.SYNTAX;
		DataDocuments data = ev.getWavelet().getDataDocuments();
		
		if ( data.contains("robot.linctex.mode") ) {
			String read = data.get("robot.linctex.mode");
			
			if (read.equals("syntax"))
				mode = Mode.SYNTAX;
			else
				mode = Mode.ANNOTATION;
		}
		
		return mode;
	}
	
	private void introduce(Event event) {
		Blip blip = event.getWavelet().reply("\nHi This is LincTex!\n");
		blip.append("\nSimply type your math expressions between double dollars like this:"+
				"\n$$\\frac{1}{2}+7x-\\sqrt{2}$$");
		blip.append("\n\nThis robot is using the forkosh.com mathtex service\n\n");
		
		FormElement btn = new FormElement(ElementType.BUTTON, "syntax");
		btn.setValue("allow $$ Syntax");
		blip.append(btn);
		
		btn = new FormElement(ElementType.BUTTON, "anno");
		btn.setValue("disable $$ Mode");
		blip.append(btn);
	}
	
}