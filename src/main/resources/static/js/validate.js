// Validator adding bitcoin address check
// jQuery.validator.addMethod("validBTC", function (value, element) {
//   return this.optional(element) || /^[13][a-zA-Z0-9]{26,33}$/.test(value);
// }, "Please enter a correct bitcoin address");

var constraints = {
  "Bitcoin Address": {
    presence: true,
    format: {
      pattern: /^[123mn][a-zA-Z0-9]{26,33}$/,
      message: "is not valid"
    }
  },
  Amount: {
    presence: true,
    numericality: {
      onlyInteger: false,
      greaterThan: 0
    }
  }

};

function checkForm() {
  var v = validate({
    "Bitcoin Address": $("#address").val(),
    Amount: $("#amount").val()
  }, constraints);
  if (v) {
    console.log(v)
    return v;
  } else {
    return false;
  }
}