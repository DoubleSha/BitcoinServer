//home
$("#showForm").click(function (e) {
  e.preventDefault();
  $("#leftLinks").hide();
  $("#form").fadeIn();
});

//form
$("#submitButton").click(function (e) {
  var apiUri = "/create",
    isInvalidForm = checkForm();
  e.preventDefault();
  if (!isInvalidForm) {
    $.ajax({
      type: "POST",
      contentType: "application/json",
      dataType: "json",
      url: apiUri,
      data: JSON.stringify({
        'address': $('#address').val() || 'invalidAddress',
        'amount': $('#amount').val() * 100000 || 0,
        'memo': $('#memo').val() || 'Click or tap "Send" to broadcast payment',
        'ackMemo': $('#ackMemo').val() || 'Thanks! Your payment is being processed by the Bitcoin network. Create your own Bitcoin link at DoubleSha.com.'
      }),
      success: function (data) {
        // This outputs the result of the ajax request
        console.log(data);
        $("#form").fadeOut();
        $("#responseURI").html(data.uri)
        $("#response").css({
          "display": "inline-block"
        });
      }
    });
  } else {
    //display errors
    if (isInvalidForm["Bitcoin Address"]) {
      alertify.error(isInvalidForm["Bitcoin Address"][0]);
    }
    if (isInvalidForm.Amount) {
      alertify.error(isInvalidForm.Amount[0]);
    }
  }
});

$("#goBack").click(function () {
  //hide response and show form
  $("#response").css({
    opacity: 0
  });
  $("#form").fadeIn();
});

$("#advanceButton").click(function () {
  // if ($("#advanceMenu").css("opacity") == 0) {
  //   openMenu();
  // } else {
  //   closeMenu();
  // }
  $("#advanceMenu").slideToggle();
});


//fancybox
$(".fancyLinks").fancybox({
  maxWidth: 800,
  maxHeight: 600,
  fitToView: false,
  width: '70%',
  height: '70%',
  autoSize: false,
  closeClick: false,
  openEffect: 'none',
  closeEffect: 'none'
});

//faq
$("#getStarted li.faq").click(function () {
  $(this).find(".answer").slideToggle();
});