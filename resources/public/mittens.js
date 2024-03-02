const pictureSrc = '/public/coffee.gif'; // RIP SHII.ORG 'http://shii.org/b/mittens.gif'; //the location of the mittens, formerly static.
const pictureWidth = 40;            //the width of the mittens
const pictureHeight = 46;           //the height of the  mittens
const numFlakes = 10;               //the number of  mittens
const downSpeed = 0.01;          
const lrFlakes = 10;         
let EmergencyMittens = false;     

//safety checks. Browsers will hang if this is wrong. If other values are wrong there will just be errors
if( typeof( numFlakes ) != 'number' || Math.round( numFlakes ) != numFlakes || numFlakes < 1 ) { numFlakes = 10; }

const style = document.createElement('style')
style.innerText = '.hidden { display: none }'
document.querySelector('head').appendChild(style)
//draw the snowflakes
for(let x = 0; x < numFlakes; x++) {
	document.write('<div style="position:absolute;z-index:1;" class="cofflake hidden" id="snFlkDiv'+x+'"><img src="'+pictureSrc+'" height="'+pictureHeight+'" width="'+pictureWidth+'" alt="*" border="0"></div>');
}

//calculate initial positions (in portions of browser window size)
let xcoords = [];
let ycoords = [];
let snFlkTemp;
for( var x = 0; x < numFlakes; x++ ) {
	xcoords[x] = ( x + 1 ) / ( numFlakes + 1 );
	do {
		snFlkTemp = Math.round( ( numFlakes - 1 ) * Math.random() );
	} while( typeof( ycoords[snFlkTemp] ) == 'number' );
	ycoords[snFlkTemp] = x / numFlakes;
}

//now animate
function mittensFall() {
	if( !getRefToDivNest('snFlkDiv0') ) { return; }
	var scrWidth = 0, scrHeight = 0, scrollHeight = 0, scrollWidth = 0;
	//find screen settings for all variations. doing this every time allows for resizing and scrolling
	if( typeof( window.innerWidth ) == 'number' ) { scrWidth = window.innerWidth; scrHeight = window.innerHeight; } else {
		if( document.documentElement && ( document.documentElement.clientWidth || document.documentElement.clientHeight ) ) {
			scrWidth = document.documentElement.clientWidth; scrHeight = document.documentElement.clientHeight; } else {
			if( document.body && ( document.body.clientWidth || document.body.clientHeight ) ) {
				scrWidth = document.body.clientWidth; scrHeight = document.body.clientHeight; } } }
	if( typeof( window.pageYOffset ) == 'number' ) { scrollHeight = pageYOffset; scrollWidth = pageXOffset; } else {
		if( document.body && ( document.body.scrollLeft || document.body.scrollTop ) ) { scrollHeight = document.body.scrollTop; scrollWidth = document.body.scrollLeft; } else {
			if( document.documentElement && ( document.documentElement.scrollLeft || document.documentElement.scrollTop ) ) { scrollHeight = document.documentElement.scrollTop; scrollWidth = document.documentElement.scrollLeft; } }
	}
	//move the snowflakes to their new position
	for( var x = 0; x < numFlakes; x++ ) {
		if( ycoords[x] * scrHeight > scrHeight - pictureHeight ) { ycoords[x] = 0; }
		var divRef = getRefToDivNest('snFlkDiv'+x); if( !divRef ) { return; }
		if( divRef.style ) { divRef = divRef.style; } var oPix = document.childNodes ? 'px' : 0;
		divRef.top = ( Math.round( ycoords[x] * scrHeight ) + scrollHeight ) + oPix;
		divRef.left = ( Math.round( ( ( xcoords[x] * scrWidth ) - ( pictureWidth / 2 ) ) + ( ( scrWidth / ( ( numFlakes + 1 ) * 4 ) ) * ( Math.sin( lrFlakes * ycoords[x] ) - Math.sin( 3 * lrFlakes * ycoords[x] ) ) ) ) + scrollWidth ) + oPix;
		ycoords[x] += downSpeed;
	}
}

//DHTML handlers
const getRefToDivNest = divName => document.querySelector(`#${divName}`)


function dispenseMittens() {
	if (EmergencyMittens) {
		console.log('lol')
	} else {
		for (const e of document.querySelectorAll('.cofflake')) {
			e.classList.remove('hidden')
		}
		EmergencyMittens = window.setInterval('mittensFall();',100);
	}
}
