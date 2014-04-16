// Validator adding bitcoin address check
// jQuery.validator.addMethod("validBTC", function (value, element) {
//   return this.optional(element) || /^[13][a-zA-Z0-9]{26,33}$/.test(value);
// }, "Please enter a correct bitcoin address");

var constraints = {
  "Bitcoin Address": {
    presence: true,
    format: {
      pattern: /^[13][a-zA-Z0-9]{26,33}$/,
      message: "is not valid"
    }
  },
  Amount: {
    presence: true,
    numericality: {
      onlyInteger: true,
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
  // => {"creditCardNumber": ["Credit card number is the wrong length (should be 16 characters)"]}

}