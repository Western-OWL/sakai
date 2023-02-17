/**
 * For Responsive Menus in Morpheus: Adds classes to the <body>
 */

function toggleToolsNav(event){
  /*global MorpheusViewportHelper*/
  /*eslint no-undef: "error"*/

  if (event) {
    event.preventDefault();
  }
  
  // The DHTML mask is only needed for mobile layouts, where the tool menu
  // will overlay much of the screen.
  if (MorpheusViewportHelper.isDesktop()) {
      return;
  }

  $PBJQ('body').toggleClass('toolsNav--displayed');
  if ($PBJQ('body').hasClass('toolsNav--displayed')) {
    /* Add the mask to grey out the top headers - re-use code in more.sites.js */
    createDHTMLMask(toggleToolsNav)
  }else{
    removeDHTMLMask();
  }
}

$PBJQ(document).ready(function(){

  $PBJQ('#roleSwitchSelect').on("change", function(){
    if( $PBJQ('option:selected', this ).text() !== '' ){
      document.location = $PBJQ('option:selected', this ).val();
    }else{
      $PBJQ(this)[0].selectedIndex = 0;
    }
  });
});

$PBJQ(".js-toggle-tools-nav", "#skipNav").on("click", toggleToolsNav);
