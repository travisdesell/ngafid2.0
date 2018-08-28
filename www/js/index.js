var base_url = "http://134.129.182.221";


function load_navbar() {
    $.get(base_url + '/templates/navbar.html', function(file_contents) {

        var view = {
            logged_in : true
        };

        var output = Mustache.render(file_contents, view);

        console.log("loading navbar!");
        $("#navbar").html(output);
    });
}


function load_main() {
    $.get(base_url + '/templates/main.html', function(file_contents) {

        var number_flights = 1035;
        var flights_per_page = 20;
        var pages = [];

        for (var i = 0; i < number_flights; i += flights_per_page) {
            pages.push( { start : i + 1, end : Math.min(number_flights, i + flights_per_page) } );
        }

        var view = {
            pages : pages
        };

        var output = Mustache.render(file_contents, view);

        console.log("loading main!");
        $("#main").html(output);
    });
}


$(document).ready(function() {
    load_navbar();
    load_main();
});
