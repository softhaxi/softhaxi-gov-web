
$(function () {
    // Initialize form validation on the registration form.
    // It has the name attribute "registration"
    $("form[name='sysParam']").validate({
        // Specify validation rules
        rules: {
            // The key name on the left side is the name attribute
            // of an input field. Validation rules are defined
            // on the right side
            code: {
                required: true,
                maxlength: 50
            },
            name: {
                required: true,
                maxlength: 100
            },
            value: {
                required: true,
                maxlength: 100
            },
        },
        // Specify validation error messages
        messages: {
            code:  {
                required: "Please enter System Parameter's code",
                maxlength: "Code must can not be more than 20 chars long"
            },
            name: {
                required: "Please enter System Parameter's name",
                maxlength: "Name must can not be more than 100 chars long"
            },
            value:  {
                required: "Please enter System Parameter's value",
                maxlength: "Value must can not be more than 100 chars long"
            }
        },
        // Make sure the form is submitted to the destination defined
        // in the "action" attribute of the form when valid
        submitHandler: function (form) {
            form.submit();
        }
    });
});
