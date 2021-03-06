/* :name = Restore ID-based Default Translations (CLI) :description=
 *
 * @author      Manuel Souto Pico
 * @date        2020-11-26
 * @version     0.0.5
 */

import org.omegat.core.events.IProjectEventListener.PROJECT_CHANGE_TYPE;
if (eventType != PROJECT_CHANGE_TYPE.CLOSE) {
     return;
}

import javax.swing.JOptionPane;
import org.omegat.util.OConsts;
import org.omegat.gui.main.ProjectUICommands;
import java.nio.file.Files;

def keep_only_srcs_with_alttgt(all_sources_set, segm_pairs, project_tmx) {

	sources_to_process = []

	all_sources_set.each { it ->

		uniq_or_first = project_tmx.body.tu.find { node ->
			(node.tuv[0].seg.text() == it) &&
			(node.prop.count{ p -> p.@type == 'id' } == 0)
		}

		if (uniq_or_first && segm_pairs[it].size() > 1) {
			// console.println("remove '${it}' from all_sources_set")
			// def f = it.parent().remove(it)
			// tusToRemove.add(uniq_or_first)

		} else {
			sources_to_process.add(it)
		}
	}
	return sources_to_process
}




    
    def prop = project.projectProperties
    if (!prop) {
    	System.Err.println('No project opened.');
    	return
    }
    
    // Keep track of the directories and files we need before closing the project.
    def projectRoot = prop.projectRootDir;
    def projectSave = new File(prop.projectInternal, OConsts.STATUS_EXTENSION)
    def timestamp = new Date().format("yyyyMMddHHmmss", TimeZone.getTimeZone('UTC'));
    def backupFile = new File(prop.projectInternal, OConsts.STATUS_EXTENSION + "." + timestamp + ".alt.bak");














    
    // Make a backup of the original file, just in case.
    // console.println("Backing up file to '${backupFile}'");
    console.println("Backing up file to " + backupFile.getName() + "\n");
    Files.copy(projectSave.toPath(), backupFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    
    // Setup the XML parser and load the project_save.tmx
    def xmlParser = new XmlParser();
    xmlParser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
    xmlParser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    def project_tmx = xmlParser.parse(projectSave);
    def tmx_body = project_tmx.body;
    
    // Get all <tu> that have a translation
    def all_sources = []
    def segm_pairs = [:]
    def segm_pairs_to_default = [:]

	// get all source texts and create a map where each source
	// text is a key and the list of all its translations is the value    
    project_tmx.body.tu.findAll { node ->
    	def source_text = node.tuv[0].seg.text()
    	def target_text = node.tuv[1].seg.text()
    
    	// add source text to list of all source texts
    	all_sources.add(source_text)
    	// create one to many mapping if it doesn't exist
    	if (!segm_pairs[source_text]) ( segm_pairs[source_text] = [] )
    	// add translation to the list that has the source text as key
    	segm_pairs[source_text].add(target_text)
    }
    
    // remove repetitions
    def all_sources_set = all_sources.toSet()






















	sources_to_process = keep_only_srcs_with_alttgt(all_sources_set, segm_pairs, project_tmx)

    
    // finds the entries that should not be alternative
    sources_to_process.each { src_txt ->
    
    	def alt_trans_set = segm_pairs[src_txt].toSet()
    	def max_rep = 0
    	// get the most frequent translation (to be made the default)
    	alt_trans_set.each { it ->
			// console.println(it)
			def freq_tgt = Collections.frequency(segm_pairs[src_txt], it)
    		//console.println("Translation '${it}' has been used ${freq_tgt} times")
    		if (freq_tgt > max_rep) {
    			max_rep = freq_tgt
    			segm_pairs_to_default[src_txt] = it
			}
		}

/*		segm_pairs_to_default.each {
			console.println(it)
		}*/
	}

	def nodes_to_default_counter = 0
    segm_pairs_to_default.each { src, tgt -> // "src, tgt" are "key, value"
    
    	// get all prop nodes that have tuv parents stored in segm_pairs_to_default
    	def nodes_to_remove = project_tmx.body.tu.prop.findAll {
    		it.parent().tuv[0].seg.find { s -> s.text() == src } && 
            it.parent().tuv[1].seg.find { s -> s.text() == tgt }
    	}

    	nodes_to_default_counter += nodes_to_remove.size()
    	nodes_to_remove.each { it ->
    		def parent = it.parent()
    		parent.remove(it)
    	}
    }
    
    // https://stackoverflow.com/a/22221811 to avoid encoding issues
    // The resulting XML file has no DOCTYPE, but it doesn't prevent it
    // to being loaded correctly.
    projectSave.withWriter("UTF-8") { w ->
    	new XmlNodePrinter( new PrintWriter( w ) ).with { p ->
    		preserveWhitespace = true;
    		p.print( project_tmx );
    	}
    }

System.out.println("Context properties removed from ${nodes_to_default_counter} entries.");
