/* :name = Restore ID-based Default Translations :description=Removes unnecessary alternative translations for unique and for most frequent repetitions by removing context (ID/filename) properties. This script can be helpful in the scenario where a project only contains ID-based alternative translations, so that auto-propagation is blocked (which makes consistent editing challenging).
 *
 * @author      Manuel Souto Pico, Briac Pilpré
 * @date        2020-08-17
 * @version     0.0.3
  */

// import org.omegat.core.events.IProjectEventListener.PROJECT_CHANGE_TYPE;
// if (eventType != PROJECT_CHANGE_TYPE.CLOSE) {
//      return;
// }

import javax.swing.JOptionPane;
import org.omegat.util.OConsts;
import org.omegat.gui.main.ProjectUICommands;
import java.nio.file.Files;

def gui() {
	final def title = 'Restore ID-based Default Translations'

	def prop = project.projectProperties
	if (!prop) {
		final def msg   = 'No project opened.'
		JOptionPane.showMessageDialog(null, msg, title, JOptionPane.INFORMATION_MESSAGE);
		return
	}

	// Keep track of the directories and files we need before closing the project.
	def projectRoot = prop.projectRootDir;
	def projectSave = new File(prop.projectInternal, OConsts.STATUS_EXTENSION)
	def timestamp = new Date().format("yyyyMMddHHmmss", TimeZone.getTimeZone('UTC'));
	def backupFile = new File(prop.projectInternal, OConsts.STATUS_EXTENSION + "." + timestamp + ".alt.bak");

	int choice = JOptionPane.showOptionDialog(null, "Default translations will be restored. Continue?",
		title,
		JOptionPane.YES_NO_OPTION,
		JOptionPane.QUESTION_MESSAGE,
		null, null, null);

	if (choice == JOptionPane.YES_OPTION)
	{
		console.print("Closing project...\n");
		// Closing project, to be safe.
		ProjectUICommands.projectClose();
	}

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

	// finds the entries that should not be alternative
	all_sources_set.each { src_txt ->

		def alt_trans_set = segm_pairs[src_txt].toSet()
		def max_rep = 0
		// get the most frequent translation (to be made the default)
		alt_trans_set.each { it ->
			def freq_tgt = Collections.frequency(segm_pairs[src_txt], it)
			//console.println("Translation '${it}' has been used ${freq_tgt} times")
			if (freq_tgt > max_rep) {
				max_rep = freq_tgt
				segm_pairs_to_default[src_txt] = it
			}
		}
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

	// Phew, all is good. Reopen the project.
	JOptionPane.showMessageDialog(null, "Context properties removed from ${nodes_to_default_counter} entries. \nPlease wait until the project is loaded again.", title, JOptionPane.INFORMATION_MESSAGE);
	ProjectUICommands.projectOpen(projectRoot, true);
}
