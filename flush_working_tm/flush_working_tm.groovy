/* :name = Flush Working TM :description=
 *
 * @author      Manuel Souto Pico
 * @date        2020-08-14
 * @version     0.0.1
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
	final def title = 'Flush working TM'

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
	def backupFile = new File(prop.projectInternal, OConsts.STATUS_EXTENSION + "." + timestamp + ".del.bak");

	int choice = JOptionPane.showOptionDialog(null, "The working TM will be flushed.  Continue?",
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
	def projectTmx = xmlParser.parse(projectSave);
	def tmxBody = projectTmx.body;

	// Find all <tu> with the tranlated <tuv> without a @changeid attribute
	def tusToRemove = [];
	projectTmx.body.tu.findAll { node ->
		def targetTuv = node.tuv[1];
		tusToRemove.add(targetTuv.parent());
	};

	// Removes the found <tu> from the XML document
	tusToRemove.each { tu ->
		def f = tu.parent().remove(tu);
	}

	// https://stackoverflow.com/a/22221811 to avoid encoding issues
	// The resulting XML file has no DOCTYPE, but it doesn't prevent it
	// to being loaded correctly.
	projectSave.withWriter("UTF-8") { w ->
		new XmlNodePrinter( new PrintWriter( w ) ).with { p ->
			preserveWhitespace = true;
			p.print( projectTmx );
		}
	}

	// Phew, all is good. Reopen the project.
	JOptionPane.showMessageDialog(null, "Working TM flushed, ${tusToRemove.size()} entries removed.", title, JOptionPane.INFORMATION_MESSAGE);
	ProjectUICommands.projectOpen(projectRoot, true);
}
