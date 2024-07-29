$(document).ready(function() {
    $.students = function() {
        $.ajax({
            method: "GET",
            url: "/api/read/"
        }).done(function(result) {
            html = "";
            for(i = 0; i < result.length; i++) {
                html += "<tr>";
                html += "<td>" + result[i].id + "</td>";
                html += "<td>" + result[i].name + "</td>";
                html += "<td>" + (result[i].fam ? result[i].fam : "") + "</td>";
                html += "<td>" + (result[i].otch ? result[i].otch : "") + "</td>";
                html += "<td>" + result[i].bdate + "</td>";
                html += "<td>" + (result[i].team ? result[i].team : "") + "</td>";
                html += "<td><a href='javascript:deleteStudent(" + result[i].id + ");'>Удалить</a></td>";
                html += "</tr>";
            }
            $(".students").html(html);
        });
    };

    $.students();

    window.deleteStudent = function(id) {
        $.ajax({
            method: "DELETE",
            url: "/api/delete/" + id
        }).done(function(result) {
            $.students();
        });
    };

    $(".validate").on("input", function() {
        var formValidator = $(this).attr("validator");
        if ($.validator[formValidator]($(this).val())) {
            $(this).removeClass("invalid");
        }
        else {
            $(this).addClass("invalid");
        }
    });

    $("form.create-student").on("submit", function(event) {
        event.preventDefault();
        var form = $(this).serializeArray();
        var req = {};
        var valid = true;
        $.map(form, function(elem, i){
            req[elem['name']] = elem['value'];
            var formElem = $("form.create-student *[name='" + elem['name'] + "']");
            var formValidator = formElem.attr('validator');
            if (formValidator) {
                if (!$.validator[formValidator](elem['value'])) {
                    valid = false;
                    formElem.addClass("invalid");
                }
            }
        });
        if (valid) {
            $.ajax({
                method: "POST",
                url: "/api/create/",
                data: JSON.stringify(req),
                processData: false
            }).done(function(result) {
                $("form.create-student").trigger("reset");
                $.students();
            }).fail(function(result) {
                alert(result.responseJSON.message);
            });
        }
    });

    $.validator = {
        word: function(value) {
            var regex = new RegExp("^(\\s*\\w+\\s*)+$", "i");
            return regex.test(value);
        },
        date: function(value) {
            var regex = new RegExp("^\\s*(\\d\\d)\\.(\\d\\d)\\.(\\d\\d\\d\\d)\\s*$", "i");
            if (!regex.test(value)) return false;
            var valueDate = value.replace(regex, "$3-$2-$1");
            var date = Date.parse(valueDate);
            return !isNaN(date);
        }
    }
});

