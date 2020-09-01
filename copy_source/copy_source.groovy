/* :name = Copy Source :description=
 *
 * @author      Manuel Souto Pico
 * @date        2020-03-03
 * @version     0.0.1
 */

def gui(){
	def segm_count = 0;

	project.allEntries.each { currSegm ->
		editor.gotoEntry(currSegm.entryNum())
		def source = currSegm.getSrcText();
		def target = project.getTranslationInfo(currSegm) ? project.getTranslationInfo(currSegm).translation : null;
          if (target == null) {
          	segm_count++;
			editor.replaceEditText(source)	
          }
	}
	console.println("Source text copied to the target segment in " + segm_count + " segments.")
}
