//home
$("#showForm").click(function (e) {
  e.preventDefault();
  $("#leftLinks").hide();
  $("#form").fadeIn();
  $("#backHome").fadeIn();
});

//form

$("#backHome").click(function (e) {
  e.preventDefault();
  $("#form").hide();
  $("#backHome").hide();
  $("#leftLinks").fadeIn();
});

$(".clickableElement").click(function () {
  open($("#bitcoinLink").href);
});

$("#submitButton").click(function (e) {
  var apiUri = "/api/create",
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
        'memo': $('#memo').val() || 'Satoshi would be proud',
        'ackMemo': $('#ackMemo').val() || 'Thanks! Your payment was received and is being processed by the Bitcoin network. Create your own Bitcoin link at DoubleSha.com.'
      }),
      success: function (data) {
        // This outputs the result of the ajax request
        $("#form").hide();
        $("#backHome").hide();
        $("#responseURI").html(data.uri);
        // $(".twitter-share").attr("data-text", 'Send Bitcoin to me at: ' + data.uri + ' @dblsha');
        $("#copyButton").attr("data-clipboard-text", data.uri);
        $("#response").fadeIn();
        $("#goBack").fadeIn();

        // Socialite.load($("#social"));

        // copy to clipboard
        ZeroClipboard.config({
          moviePath: 'ZeroClipboard.swf',
        });
        var client = new ZeroClipboard($("#copyButton"));

        // client.on("ready", function (readyEvent) {
        //   console.log("ZeroClipboard SWF is ready!");

        //   client.on('dataRequested', function (client, args) {
        //     client.setText(data.uri);
        //   });

        //   client.on("aftercopy", function (event) {
        //     console.log("Copied text to clipboard: " + event.data["text/plain"]);
        //   });
        // });

        //tipsy
        // $('#copyButton').tipsy({
        //   gravity: 'w'
        // });

        // $("#copyButton").click(function (e) {
        //   e.preventDefault();
        //   $("#copyButton").attr("title", "copied");
        // });
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
  $("#response").hide();
  $("#goBack").hide();
  $("#form").fadeIn();
  $("#backHome").fadeIn();
  //clear fields
  $("#address").val("");
  $("#amount").val("");
  $("#memo").val("");
  $("#ackMemo").val("");
});

$("#advanceButton").click(function () {
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