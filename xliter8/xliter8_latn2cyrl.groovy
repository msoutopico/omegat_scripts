/* :name = Xliterate Serbian Project (Latin to Cyrillic) :description=Transliterates globally in the project any Latin letter, if found, to its Cyrillic counterpart 
 * 
 * @author      Manuel Souto Pico, Briac Pilpré
 * @date        2019-12-27 (updated 2020-02-01, 2020-03-16)
 * @version     0.1.6 (for Serbian)
 * @inspìration from https://stackoverflow.com/questions/43088951/groovy-transliteration-any-language-to-latin 
 * @documen	http://userguide.icu-project.org/transforms/general
 * @documen 	http://www.icu-project.org/apiref/icu4j/com/ibm/icu/text/Transliterator.html
 */

@Grab(group='com.ibm.icu', module='icu4j', version='58.2')
import com.ibm.icu.text.Transliterator;

transliteratorId = 'Latin-Cyrillic';

//def text = '<t0/>';		console.println(text + ' => ' + transliterate(transliteratorId, text));


def gui(){
	def segm_count = 0;

	project.allEntries.each { currSegm ->
		editor.gotoEntry(currSegm.entryNum())
		def target = project.getTranslationInfo(currSegm) ? project.getTranslationInfo(currSegm).translation : null;

		if (target == null) {
			console.println("----");
			return ;
		}

		def newTarget = transliterate(transliteratorId, target);
		newTarget = refine_xlit(newTarget);
		segm_count++;
		console.println(currSegm.entryNum() + "\t" + target + " => " + newTarget)
		editor.replaceEditText(newTarget)
	}
	console.println(segm_count + " segments modified")
}

def refine_xlit(text){

	/* key is generic Cyrillic, value is specific Serbian character */
	def map = [
				// "W": "В", "w": "в",
				"Й":"Ј", "й":"ј", 
				"Лј":"Љ", "лј":"љ", "ЛЈ":"Љ",
				"Нј":"Њ", "нј":"њ", "НЈ":"Њ",
				"Дж":"Џ", "дж":"џ", "ДЖ":"Џ"
			]

	map.each { cyr, srp ->
		// console.println "${cyr} => ${srp}"
		search = /${cyr}/
		replac = "${srp}"
		text = text.replaceAll(search, replac)
	}
	return text;
}


def transliterate(transliterateId, text) {
	def filter = /(\s*<[^>]+>\s*)+/;
	def transliterator = Transliterator.getInstance(transliterateId);

	def tags = [];
	text.eachMatch(filter) { t -> tags.add(t[0]) }

	def chunks = []
	for (String chunk : text.split(filter)) {
		chunks.add(transliterator.transform(chunk));
	}

	def merge = []

	if (chunks.size == 0 && tags.size > 0) {
		merge.add(tags[0]);
	}
	else if (chunks.size > 0) {
		for (def i = 0; i < chunks.size; i++) {
			merge.add(chunks[i] + (tags[i] ?: ''));
		}
	}
	else {
		console.println("** WARN ** [" + text + "]");
		return text;
	}

	return merge.join('')
}
