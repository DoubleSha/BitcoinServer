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
        $("#responseURI").html(data.uri);
        $("#tShare").attr("data-url", data.uri);
        $("#response").css({
          opacity: 1
        });

        //copy to clipboard stuff
        // copy to clipboard
        ZeroClipboard.config({
          moviePath: 'ZeroClipboard.swf',
          trustedDomains: [window.location.host],
        });
        var client = new ZeroClipboard($("#copyButton"));

        client.on("ready", function (readyEvent) {
          console.log("ZeroClipboard SWF is ready!");

          client.on('dataRequested', function (client, args) {
            client.setText(data.uri);
          });

          client.on("aftercopy", function (event) {
            console.log("Copied text to clipboard: " + event.data["text/plain"]);
          });
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
  if ($("#advanceMenu").css("opacity") == 0) {
    openMenu();
  } else {
    closeMenu();
  }
});

//social sharing
$(document).ready(function () {
  Socialite.load($(this)[0]);
})