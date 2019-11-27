Encoder = {

	// When encoding do we convert characters into html or numerical entities
	EncodeType : "entity",  // entity OR numerical

	isEmpty : function(val){
		if(val){
			return ((val===null) || val.length===0 || /^\s+$/.test(val));
		}else{
			return true;
		}
	},

	// arrays for conversion from HTML Entities to Numerical values
	arr1: ['&nbsp;','&iexcl;','&cent;','&pound;','&curren;','&yen;','&brvbar;','&sect;','&uml;','&copy;','&ordf;','&laquo;','&not;','&shy;','&reg;','&macr;','&deg;','&plusmn;','&sup2;','&sup3;','&acute;','&micro;','&para;','&middot;','&cedil;','&sup1;','&ordm;','&raquo;','&frac14;','&frac12;','&frac34;','&iquest;','&Agrave;','&Aacute;','&Acirc;','&Atilde;','&Auml;','&Aring;','&AElig;','&Ccedil;','&Egrave;','&Eacute;','&Ecirc;','&Euml;','&Igrave;','&Iacute;','&Icirc;','&Iuml;','&ETH;','&Ntilde;','&Ograve;','&Oacute;','&Ocirc;','&Otilde;','&Ouml;','&times;','&Oslash;','&Ugrave;','&Uacute;','&Ucirc;','&Uuml;','&Yacute;','&THORN;','&szlig;','&agrave;','&aacute;','&acirc;','&atilde;','&auml;','&aring;','&aelig;','&ccedil;','&egrave;','&eacute;','&ecirc;','&euml;','&igrave;','&iacute;','&icirc;','&iuml;','&eth;','&ntilde;','&ograve;','&oacute;','&ocirc;','&otilde;','&ouml;','&divide;','&oslash;','&ugrave;','&uacute;','&ucirc;','&uuml;','&yacute;','&thorn;','&yuml;','&quot;','&amp;','&lt;','&gt;','&OElig;','&oelig;','&Scaron;','&scaron;','&Yuml;','&circ;','&tilde;','&ensp;','&emsp;','&thinsp;','&zwnj;','&zwj;','&lrm;','&rlm;','&ndash;','&mdash;','&lsquo;','&rsquo;','&sbquo;','&ldquo;','&rdquo;','&bdquo;','&dagger;','&Dagger;','&permil;','&lsaquo;','&rsaquo;','&euro;','&fnof;','&Alpha;','&Beta;','&Gamma;','&Delta;','&Epsilon;','&Zeta;','&Eta;','&Theta;','&Iota;','&Kappa;','&Lambda;','&Mu;','&Nu;','&Xi;','&Omicron;','&Pi;','&Rho;','&Sigma;','&Tau;','&Upsilon;','&Phi;','&Chi;','&Psi;','&Omega;','&alpha;','&beta;','&gamma;','&delta;','&epsilon;','&zeta;','&eta;','&theta;','&iota;','&kappa;','&lambda;','&mu;','&nu;','&xi;','&omicron;','&pi;','&rho;','&sigmaf;','&sigma;','&tau;','&upsilon;','&phi;','&chi;','&psi;','&omega;','&thetasym;','&upsih;','&piv;','&bull;','&hellip;','&prime;','&Prime;','&oline;','&frasl;','&weierp;','&image;','&real;','&trade;','&alefsym;','&larr;','&uarr;','&rarr;','&darr;','&harr;','&crarr;','&lArr;','&uArr;','&rArr;','&dArr;','&hArr;','&forall;','&part;','&exist;','&empty;','&nabla;','&isin;','&notin;','&ni;','&prod;','&sum;','&minus;','&lowast;','&radic;','&prop;','&infin;','&ang;','&and;','&or;','&cap;','&cup;','&int;','&there4;','&sim;','&cong;','&asymp;','&ne;','&equiv;','&le;','&ge;','&sub;','&sup;','&nsub;','&sube;','&supe;','&oplus;','&otimes;','&perp;','&sdot;','&lceil;','&rceil;','&lfloor;','&rfloor;','&lang;','&rang;','&loz;','&spades;','&clubs;','&hearts;','&diams;'],
	arr2: ['&#160;','&#161;','&#162;','&#163;','&#164;','&#165;','&#166;','&#167;','&#168;','&#169;','&#170;','&#171;','&#172;','&#173;','&#174;','&#175;','&#176;','&#177;','&#178;','&#179;','&#180;','&#181;','&#182;','&#183;','&#184;','&#185;','&#186;','&#187;','&#188;','&#189;','&#190;','&#191;','&#192;','&#193;','&#194;','&#195;','&#196;','&#197;','&#198;','&#199;','&#200;','&#201;','&#202;','&#203;','&#204;','&#205;','&#206;','&#207;','&#208;','&#209;','&#210;','&#211;','&#212;','&#213;','&#214;','&#215;','&#216;','&#217;','&#218;','&#219;','&#220;','&#221;','&#222;','&#223;','&#224;','&#225;','&#226;','&#227;','&#228;','&#229;','&#230;','&#231;','&#232;','&#233;','&#234;','&#235;','&#236;','&#237;','&#238;','&#239;','&#240;','&#241;','&#242;','&#243;','&#244;','&#245;','&#246;','&#247;','&#248;','&#249;','&#250;','&#251;','&#252;','&#253;','&#254;','&#255;','&#34;','&#38;','&#60;','&#62;','&#338;','&#339;','&#352;','&#353;','&#376;','&#710;','&#732;','&#8194;','&#8195;','&#8201;','&#8204;','&#8205;','&#8206;','&#8207;','&#8211;','&#8212;','&#8216;','&#8217;','&#8218;','&#8220;','&#8221;','&#8222;','&#8224;','&#8225;','&#8240;','&#8249;','&#8250;','&#8364;','&#402;','&#913;','&#914;','&#915;','&#916;','&#917;','&#918;','&#919;','&#920;','&#921;','&#922;','&#923;','&#924;','&#925;','&#926;','&#927;','&#928;','&#929;','&#931;','&#932;','&#933;','&#934;','&#935;','&#936;','&#937;','&#945;','&#946;','&#947;','&#948;','&#949;','&#950;','&#951;','&#952;','&#953;','&#954;','&#955;','&#956;','&#957;','&#958;','&#959;','&#960;','&#961;','&#962;','&#963;','&#964;','&#965;','&#966;','&#967;','&#968;','&#969;','&#977;','&#978;','&#982;','&#8226;','&#8230;','&#8242;','&#8243;','&#8254;','&#8260;','&#8472;','&#8465;','&#8476;','&#8482;','&#8501;','&#8592;','&#8593;','&#8594;','&#8595;','&#8596;','&#8629;','&#8656;','&#8657;','&#8658;','&#8659;','&#8660;','&#8704;','&#8706;','&#8707;','&#8709;','&#8711;','&#8712;','&#8713;','&#8715;','&#8719;','&#8721;','&#8722;','&#8727;','&#8730;','&#8733;','&#8734;','&#8736;','&#8743;','&#8744;','&#8745;','&#8746;','&#8747;','&#8756;','&#8764;','&#8773;','&#8776;','&#8800;','&#8801;','&#8804;','&#8805;','&#8834;','&#8835;','&#8836;','&#8838;','&#8839;','&#8853;','&#8855;','&#8869;','&#8901;','&#8968;','&#8969;','&#8970;','&#8971;','&#9001;','&#9002;','&#9674;','&#9824;','&#9827;','&#9829;','&#9830;'],

	// Convert HTML entities into numerical entities
	HTML2Numerical : function(s){
		return this.swapArrayVals(s,this.arr1,this.arr2);
	},

	// Convert Numerical entities into HTML entities
	NumericalToHTML : function(s){
		return this.swapArrayVals(s,this.arr2,this.arr1);
	},


	// Numerically encodes all unicode characters
	numEncode : function(s){
		if(this.isEmpty(s)) return "";

		var a = [],
			l = s.length;

		for (var i=0;i<l;i++){
			var c = s.charAt(i);
			if (c < " " || c > "~"){
				a.push("&#");
				a.push(c.charCodeAt()); //numeric value of code point
				a.push(";");
			}else{
				a.push(c);
			}
		}

		return a.join("");
	},

	// HTML Decode numerical and HTML entities back to original values
	htmlDecode : function(s){

		var c,m,d = s;

		if(this.isEmpty(d)) return "";

		// convert HTML entites back to numerical entites first
		d = this.HTML2Numerical(d);

		// look for numerical entities &#34;
		arr=d.match(/&#[0-9]{1,5};/g);

		// if no matches found in string then skip
		if(arr!==null){
			for(var x=0;x<arr.length;x++){
				m = arr[x];
				c = m.substring(2,m.length-1); //get numeric part which is refernce to unicode character
				// if its a valid number we can decode
				if(c >= -32768 && c <= 65535){
					// decode every single match within string
					d = d.replace(m, String.fromCharCode(c));
				}else{
					d = d.replace(m, ""); //invalid so replace with nada
				}
			}
		}

		return d;
	},

	// encode an input string into either numerical or HTML entities
	htmlEncode : function(s,dbl){

		if(this.isEmpty(s)) return "";

		// do we allow double encoding? E.g will &amp; be turned into &amp;amp;
		dbl = dbl || false; //default to prevent double encoding

		// if allowing double encoding we do ampersands first
		if(dbl){
			if(this.EncodeType==="numerical"){
				s = s.replace(/&/g, "&#38;");
			}else{
				s = s.replace(/&/g, "&amp;");
			}
		}

		// convert the xss chars to numerical entities ' " < >
		s = this.XSSEncode(s,false);

		if(this.EncodeType==="numerical" || !dbl){
			// Now call function that will convert any HTML entities to numerical codes
			s = this.HTML2Numerical(s);
		}

		// Now encode all chars above 127 e.g unicode
		s = this.numEncode(s);

		// now we know anything that needs to be encoded has been converted to numerical entities we
		// can encode any ampersands & that are not part of encoded entities
		// to handle the fact that I need to do a negative check and handle multiple ampersands &&&
		// I am going to use a placeholder

		// if we don't want double encoded entities we ignore the & in existing entities
		if(!dbl){
			s = s.replace(/&#/g,"##AMPHASH##");

			if(this.EncodeType==="numerical"){
				s = s.replace(/&/g, "&#38;");
			}else{
				s = s.replace(/&/g, "&amp;");
			}

			s = s.replace(/##AMPHASH##/g,"&#");
		}

		// replace any malformed entities
		s = s.replace(/&#\d*([^\d;]|$)/g, "$1");

		if(!dbl){
			// safety check to correct any double encoded &amp;
			s = this.correctEncoding(s);
		}

		// now do we need to convert our numerical encoded string into entities
		if(this.EncodeType==="entity"){
			s = this.NumericalToHTML(s);
		}

		return s;
	},

	// Encodes the basic 4 characters used to malform HTML in XSS hacks
	XSSEncode : function(s,en){
		if(!this.isEmpty(s)){
			en = en || true;
			// do we convert to numerical or html entity?
			if(en){
				s = s.replace(/\'/g,"&#39;"); //no HTML equivalent as &apos is not cross browser supported
				s = s.replace(/\"/g,"&quot;");
				s = s.replace(/</g,"&lt;");
				s = s.replace(/>/g,"&gt;");
			}else{
				s = s.replace(/\'/g,"&#39;"); //no HTML equivalent as &apos is not cross browser supported
				s = s.replace(/\"/g,"&#34;");
				s = s.replace(/</g,"&#60;");
				s = s.replace(/>/g,"&#62;");
			}
			return s;
		}else{
			return "";
		}
	},

	// returns true if a string contains html or numerical encoded entities
	hasEncoded : function(s){
		if(/&#[0-9]{1,5};/g.test(s)){
			return true;
		}else if(/&[A-Z]{2,6};/gi.test(s)){
			return true;
		}else{
			return false;
		}
	},

	// will remove any unicode characters
	stripUnicode : function(s){
		return s.replace(/[^\x20-\x7E]/g,"");

	},

	// corrects any double encoded &amp; entities e.g &amp;amp;
	correctEncoding : function(s){
		return s.replace(/(&amp;)(amp;)+/,"$1");
	},


	// Function to loop through an array swaping each item with the value from another array e.g swap HTML entities with Numericals
	swapArrayVals : function(s,arr1,arr2){
		if(this.isEmpty(s)) return "";
		var re;
		if(arr1 && arr2){
			//ShowDebug("in swapArrayVals arr1.length = " + arr1.length + " arr2.length = " + arr2.length)
			// array lengths must match
			if(arr1.length === arr2.length){
				for(var x=0,i=arr1.length;x<i;x++){
					re = new RegExp(arr1[x], 'g');
					s = s.replace(re,arr2[x]); //swap arr1 item with matching item from arr2
				}
			}
		}
		return s;
	},

	inArray : function( item, arr ) {
		for ( var i = 0, x = arr.length; i < x; i++ ){
			if ( arr[i] === item ){
				return i;
			}
		}
		return -1;
	}
};

////////////////////////////////////////////////////
// wordWindow object
////////////////////////////////////////////////////
function wordWindow() {
	// private properties
	this._forms = [];

	// private methods
	this._getWordObject = _getWordObject;
	//this._getSpellerObject = _getSpellerObject;
	this._wordInputStr = _wordInputStr;
	this._adjustIndexes = _adjustIndexes;
	this._isWordChar = _isWordChar;
	this._lastPos = _lastPos;

	// public properties
	this.wordChar = /[a-zA-Z]/;
	this.windowType = "wordWindow";
	this.originalSpellings = new Array();
	this.suggestions = new Array();
	this.checkWordBgColor = "pink";
	this.normWordBgColor = "white";
	this.text = "";
	this.textInputs = new Array();
	this.indexes = new Array();
	//this.speller = this._getSpellerObject();

	// public methods
	this.resetForm = resetForm;
	this.totalMisspellings = totalMisspellings;
	this.totalWords = totalWords;
	this.totalPreviousWords = totalPreviousWords;
	//this.getTextObjectArray = getTextObjectArray;
	this.getTextVal = getTextVal;
	this.setFocus = setFocus;
	this.removeFocus = removeFocus;
	this.setText = setText;
	//this.getTotalWords = getTotalWords;
	this.writeBody = writeBody;
	this.printForHtml = printForHtml;
}

function resetForm() {
	if( this._forms ) {
		for( var i = 0; i < this._forms.length; i++ ) {
			this._forms[i].reset();
		}
	}
	return true;
}

function totalMisspellings() {
	var total_words = 0;
	for( var i = 0; i < this.textInputs.length; i++ ) {
		total_words += this.totalWords( i );
	}
	return total_words;
}

function totalWords( textIndex ) {
	return this.originalSpellings[textIndex].length;
}

function totalPreviousWords( textIndex, wordIndex ) {
	var total_words = 0;
	for( var i = 0; i <= textIndex; i++ ) {
		for( var j = 0; j < this.totalWords( i ); j++ ) {
			if( i === textIndex && j === wordIndex ) {
				break;
			} else {
				total_words++;
			}
		}
	}
	return total_words;
}

//function getTextObjectArray() {
//	return this._form.elements;
//}

function getTextVal( textIndex, wordIndex ) {
	var word = this._getWordObject( textIndex, wordIndex );
	if( word ) {
		return word.value;
	}
}

function setFocus( textIndex, wordIndex ) {
	var word = this._getWordObject( textIndex, wordIndex );
	if( word ) {
		if( word.type === "text" ) {
			word.focus();
			word.style.backgroundColor = this.checkWordBgColor;
		}
	}
}

function removeFocus( textIndex, wordIndex ) {
	var word = this._getWordObject( textIndex, wordIndex );
	if( word ) {
		if( word.type === "text" ) {
			word.blur();
			word.style.backgroundColor = this.normWordBgColor;
		}
	}
}

function setText( textIndex, wordIndex, newText ) {
	var word = this._getWordObject( textIndex, wordIndex );
	var beginStr;
	var endStr;
	if( word ) {
		var pos = this.indexes[textIndex][wordIndex];
		var oldText = Encoder.htmlEncode(word.value);
		// update the text given the index of the string
		beginStr = this.textInputs[textIndex].substring( 0, pos );
		endStr = this.textInputs[textIndex].substring(
			pos + oldText.length,
			this.textInputs[textIndex].length
		);
		this.textInputs[textIndex] = beginStr + newText + endStr;

		// adjust the indexes on the stack given the differences in
		// length between the new word and old word.
		var lengthDiff = newText.length - oldText.length;
		this._adjustIndexes( textIndex, wordIndex, lengthDiff );

		word.size = newText.length;
		word.value = newText;
		this.removeFocus( textIndex, wordIndex );
	}
}


/* This should be more robust for extracting words from HTML.
 * Many repercussions = project for another time; possibly
 * make it a server side project with return of everything
 * already done where only client side change (other than deleting
 * the way we're processing) would be insertion of corrections into
 * possible html markup within original spellings OR do it ALL
 * here and give the server nothing but plain text words - MWB
 * (Either way, give only plain text to spell checker)
 */
// redesigned to handle plain text search failure to find words
// returned by server
function writeBody() {
	var d = window.document;
	d.open();

	// iterate through each text input.
	for( var txtid = 0; txtid < this.textInputs.length; txtid++ ) {
		d.writeln( '<form name="textInput'+txtid+'">' );
		var wordtxt = this.textInputs[txtid];
		//this.indexes[txtid] = []; // we'll copy after this is known
		if( wordtxt ) {
			var orig = this.originalSpellings[txtid];
			if( !orig ) break;

/* Create array of word locations
 *
 * Following logic is based on four assumptions:
 * 1. Server always returns misspelled words in doc sequence
 * 2. We'll never find a misspell occurrence the server didn't find.
 *    (but handle the exception if it happens)
 * 3. Our plain text method of locating words the server found can fail
 * 4. HTML is always valid
 *
 * We end up with arrays of only those words with their locations we could find
 * using plain text search method.
 */
			var i,j,k;

			// initialize our locations working structure:
			//    serverTotal/dupFlag, totalFound/dupRef, positionArray/position
			var locations = new Array(orig.length);
			for(i=0;i<locations.length;i++) locations[i] = new Array(1,0,null);

			// now mark multiple misspell occurrences the server found
			for(i=0;i<locations.length;i++) {
				if(locations[i][0] === -1) continue; // already dup of a previous word
					for(j=i+1;j<locations.length;j++) {
						if(orig[j]===orig[i]) {
							locations[i][0]++;    // add up number server found
							locations[j][0] = -1; // mark as dup reference
							locations[j][1] = i;  // reference to first occurrence
						}
					}
			}

			// find all misspell locations we can with verbatim text search for each unique
			// word and only search between tags.
			// end up with locations[i][2] == Array of all positions we found word
			var keepLooking;
			var end_idx;
			var begin_idx;
			var tagNextStart;
			for(i=0;i<locations.length;i++) {
				if(locations[i][0] === -1) continue; // dup, we've already done this word
				locations[i][2] = new Array();
				keepLooking = true;
				end_idx = 0;
				tagNextStart = wordtxt.indexOf("<"); // we only look between tags
				if(tagNextStart === -1) tagNextStart = wordtxt.length; // no tags
				do {
					begin_idx = wordtxt.indexOf( orig[i], end_idx );
					if(begin_idx === -1) keepLooking = false;
					else if(tagNextStart<begin_idx) { // prevents getting a FUBAR doc
						end_idx=wordtxt.indexOf(">",tagNextStart+1)+1; // always found if valid html
						tagNextStart=wordtxt.indexOf("<",end_idx);
						if(tagNextStart === -1) tagNextStart=wordtxt.length; // no more tags
					}
					else if( !this._isWordChar(wordtxt.charAt(begin_idx+orig[i].length)) &&
							!this._isWordChar(wordtxt.charAt(begin_idx-1)) ) {
						locations[i][2].push(begin_idx);
						end_idx = begin_idx + orig[i].length + 1;
					}
					else end_idx = begin_idx + orig[i].length + 1;
				} while(keepLooking);
				locations[i][1] = locations[i][2].length;
				// Enforce one of our assumptions. This 'should' never happen, but if
				// we found more occurances of any word than the server found,
				// prevent mess up by getting rid of em
				if(locations[i][1]>locations[i][0]) locations[i][1]=0;
			}

			// Define all locations that have only one possibility.
			// Those for which server and we found the same number of
			// misspell occurrences for given misspelling of a word.
			for(i=0;i<locations.length;i++) {
				if(locations[i][0]===locations[i][1] && typeof(locations[i][2])==="object") {
					locations[i][0]=1;
					locations[i][1]=1;
					var foundarray=locations[i][2];
					locations[i][2]=foundarray.shift();
					for(j=i+1;j<locations.length && foundarray.length>0;j++) {
						if(locations[j][0] === -1 && locations[j][1]===i) {
							locations[j][0]=1; // total 1
							locations[j][1]=1; // found 1
							locations[j][2]=foundarray.shift(); // the location
						}
					}
				}
			}

			// now reduce multiple possibilities (never a known case when using Aspell)
			//
			// Extract array of referenced words of which we've found
			// at least one that have more locations in the original
			// sequence than we could find
			var multiwords = new Array(); // each item = [ref, docPosition]
			for(i=0;i<locations.length;i++) {
				if(locations[i][0]>locations[i][1] && locations[i][1]>0) {
					for(j=0;j<locations[i][2].length;j++) {
						multiwords.push(new Array(i,locations[i][2][j]));
					}
					locations[i][0] = -1; // mark as referenced after we've extracted info
					locations[i][1] = i;
				}
			}

			// now sort this array by sequence we found in doc (presumably like server)
			multiwords.sort(new Function("a","b","return a[1]-b[1]"));

			// Shift each location of this array to the location
			// they fit in original server sequence
			var keepLooking = true;
			var maxcheck = true;
			var minIdx = 0;
			var minLoc = 0;
			for(j=0;j<multiwords.length;j++) {
				// check each position for this word in doc order; look for
				// word location to pass minimum criteria, then look for upper
				// criteria. if it doesn't fit, go to the next occurrence
				for(keepLooking=true,i=minIdx;i<locations.length && keepLooking;i++) {
					if(locations[i][0]===-1 && locations[i][1]===multiwords[j][0]) { // if a ref
						if(multiwords[j][1]>minLoc) { // if beyond last word
							for(maxcheck=true,k=i+1;k<locations.length && maxcheck;k++) {
								if(locations[k][0] !== -1 && locations[k][1]>0) {
									if(locations[k][2]>multiwords[j][1]) { // and if before next word
										locations[i][0] = 1;
										locations[i][1] = 1;
										locations[i][2] = multiwords[j][1];
										minIdx = i+1; // no point starting next word search before here
										keepLooking=false;
									}
									maxcheck=false;
								}
							}
							if(maxcheck) { //nothing is after this location
								locations[i][0] = 1;
								locations[i][1] = 1;
								locations[i][2] = multiwords[j][1];
								minIdx = i+1;
								keepLooking=false;
							}
						}
					}
					else if(locations[i][0] !== -1) {
						minLoc=locations[i][2];
						minIdx=i+1; // where to start next word search
					}
				}
			}

			// splice arrays to get rid of unfound words and their suggestions
			// (should only omit words that plain text search can't find)
			for(i=locations.length-1;i>=0;i--) { // reverse is simpler
				if(locations[i][0] === -1 || locations[i][1]===0) { // unfound ref OR none found
					locations.splice(i,1);
					this.originalSpellings[txtid].splice(i,1);
					this.suggestions[txtid].splice(i,1);
				}
			}

			// finally, write out the doc and word locations
			this.indexes[txtid]= new Array(locations.length);
			d.writeln('<div class="plainText">');
			for(minLoc=0,i=0;i<locations.length;i++) {
				this.indexes[txtid][i] = locations[i][2];
				d.write(wordtxt.substring(minLoc,locations[i][2])); // before word
				d.write( this._wordInputStr(this.originalSpellings[txtid][i],txtid,i)); // the word
				minLoc = locations[i][2]+this.originalSpellings[txtid][i].length; // where to write from next
			}
			d.write(wordtxt.substring(minLoc)); // end of doc
			d.writeln('</div>');
			d.writeln('</form>');
		}
	}
	d.close();

	// set the _forms property
	this._forms = d.forms;

	// Replace all hyperlinks with spans without the href's that look like links.
	// This prevents being able to break it by navigating the wordWindow with links.
	var find = /<a(\s[^\>]*)href=\"[^\"]*\"(.*?)\<\/a\>/gi;
	var repl = '<span style="color:blue;text-decoration:underline"$1$2</span>';
	// memory leak for IE?
	//d.body.innerHTML = d.body.innerHTML.replace(find,repl);
	var doc = d.body.innerHTML.replace(find,repl);
	d.body.innerHTML = doc;
}

// return the character index in the full text after the last word we evaluated
function _lastPos( txtid, idx ) {
	if( idx > 0 )
		return this.indexes[txtid][idx-1] + this.originalSpellings[txtid][idx-1].length;
	else
		return 0;
}

function printForHtml( n ) {
	return n ;		// by FredCK
/*
	var htmlstr = n;
	if( htmlstr.length == 1 ) {
		// do simple case statement if it's just one character
		switch ( n ) {
			case "\n":
				htmlstr = '<br/>';
				break;
			case "<":
				htmlstr = '&lt;';
				break;
			case ">":
				htmlstr = '&gt;';
				break;
		}
		return htmlstr;
	} else {
		htmlstr = htmlstr.replace( /</g, '&lt' );
		htmlstr = htmlstr.replace( />/g, '&gt' );
		htmlstr = htmlstr.replace( /\n/g, '<br/>' );
		return htmlstr;
	}
*/
}

function _isWordChar( letter ) {
	if( letter.search( this.wordChar ) === -1 ) {
		return false;
	} else {
		return true;
	}
}

function _getWordObject( textIndex, wordIndex ) {
	if( this._forms[textIndex] ) {
		if( this._forms[textIndex].elements[wordIndex] ) {
			return this._forms[textIndex].elements[wordIndex];
		}
	}
	return null;
}

function _wordInputStr( word ) {
	var str = '<input readonly ';
	str += 'class="blend" type="text" value="' + word + '" size="' + word.length + '">';
	return str;
}

function _adjustIndexes( textIndex, wordIndex, lengthDiff ) {
	for( var i = wordIndex + 1; i < this.originalSpellings[textIndex].length; i++ ) {
		this.indexes[textIndex][i] = this.indexes[textIndex][i] + lengthDiff;
	}
}
