/* :name = Flush Unsigned Entries (CLI) :description=Removes any entries from project_save.tmx which are not "signed" by the user, to fix the issue in OmegaT that erases alternative or enforced translations
 *
 * @author      Briac Pilpré, Manuel Souto Pico
 * @date        2020-06-30
 * @version     0.2.1
 */

import org.omegat.core.events.IProjectEventListener.PROJECT_CHANGE_TYPE;
if (eventType != PROJECT_CHANGE_TYPE.CLOSE) {
	return;
}

import javax.swing.JOptionPane;
import org.omegat.util.OConsts;
import org.omegat.gui.main.ProjectUICommands;
import java.nio.file.Files;


def prop = project.projectProperties
if (!prop) {
	System.Err.println('No project opened.');
	return;
}

// Keep track of the directories and files we need before closing the project.
def projectRoot = prop.projectRootDir;
def projectSave = new File(prop.projectInternal, OConsts.STATUS_EXTENSION)

def timestamp = new Date().format("yyyyMMddHHmmss", TimeZone.getTimeZone('UTC'));

def backupFile = new File(prop.projectInternal, OConsts.STATUS_EXTENSION + ".fix" + timestamp + ".bak");

// ProjectUICommands.projectClose();
// org.omegat.core.Core.getProject().saveProject(true);
// org.omegat.core.data.ProjectFactoryProjectFactory.closeProject();

// Make a backup of the original file, just in case.
console.println("Backing up file to '${backupFile}'");
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
	def propFile    = node?.prop.find  {  p -> p.@type    == 'file' }?.text()
	def propId      = node?.prop.find  {  p -> p.@type    == 'id'   }?.text()
	def targetTuv = node.tuv[1];
	if (!targetTuv.@changeid && !propFile && !propId) {
		tusToRemove.add(targetTuv.parent());
	}
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


System.out.println("Project cleaned, ${tusToRemove.size()} segments removed.");
