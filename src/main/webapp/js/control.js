$("#submitButton").click(function () {
  var apiUri = "/create";
  $.ajax({
    type: "POST",
    contentType: "application/json",
    dataType: "json",
    url: apiUri,
    data: JSON.stringify({
      'address': $('#address').val() || 'invalidAddress',
      'amount': $('#amount').val() * 100000 || 0,
      'network': 'test',
      'memo': $('#memo').val() || 'default memo',
      'ackMemo': $('#ackMemo').val() || 'default ackmemo'
    }),
    success: function (data) {
      // This outputs the result of the ajax request
      console.log(data);
      $("#form").fadeOut();
      $("#responseURI").html(data.uri)
      $("#response").css({
        opacity: 1
      });
    }
  });
});

$("#goBack").click(function () {
  //hide response and show form
  $("#response").css({
    opacity: 0
  });
  $("#form").fadeIn();
});

$("#advanceButton").click(function () {
  if ($("#advanceMenu").css("opacity") == 0) {
    openMenu();
  } else {
    closeMenu();
  }
});