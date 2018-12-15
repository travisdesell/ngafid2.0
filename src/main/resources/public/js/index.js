var base_url = "http://134.129.182.221";


function load_navbar() {
    $.get(base_url + '/templates/navbar.html', function(file_contents) {

        var view = {
            logged_in : false
        };

        var output = Mustache.render(file_contents, view);

        console.log("loading navbar!");
        $("#navbar").html(output);
    });
}


function load_main() {
    $.get(base_url + '/templates/main.html', function(file_contents) {

        var number_flights = 25;
        var flights_per_page = 20;
        var flights = [];

        for (var i = 0; i < number_flights; i += flights_per_page) {
            flights.push({
                airframe : "Piper Archer",
                itinerary : {
                }
            });
        }

        var view = {
            flights : flights
        };

        var output = Mustache.render(file_contents, view);

        console.log("loading main!");
        $("#main").html(output);
    });
}

$(document).ready(function() {
    console.log("document ready!");

    load_navbar();
    load_main();

    $(window).scroll(function() {
        console.log("scrolling, top: " + $(window).scrollTop());

        if ($(window).scrollTop() == $(document).height() - $(window).height()) {
            // ajax call get data from server and append to the div
            $("#load-more").html("Load More");
            console.log("loading more!");
        }
    });


});
