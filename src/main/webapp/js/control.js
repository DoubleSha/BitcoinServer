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
  } else {
    //display errors
    if (isInvalidForm.address) {
      console.log("something wrong with address");
    }
    if (isInvalidForm.amount) {
      console.log("something wrong with amount");
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
  if ($("#advanceMenu").css("opacity") == 0) {
    openMenu();
  } else {
    closeMenu();
  }
});