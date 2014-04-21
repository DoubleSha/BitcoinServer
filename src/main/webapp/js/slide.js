function openMenu() {
  console.log('open menu');
  $('#advanceMenu').css('opacity', '1');
  $('#advanceMenu').animate({
    height: '209px'
  }, 400, 'easeOutBack');
}

function closeMenu() {
  console.log('close menu');

  $('#advanceMenu').animate({
    height: '0px'
  }, 400, 'easeOutQuint');
  $('#advanceMenu').css('opacity', '0');
}