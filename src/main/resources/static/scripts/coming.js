function processTimer(deadline){
    var time = deadline - new Date();
    return {
        'days': Math.floor( time/(1000*60*60*24) ),
        'hours': Math.floor( (time/(1000*60*60)) % 24 ),
        'minutes': Math.floor( (time/1000/60) % 60 ),
        'seconds': Math.floor( (time/1000) % 60 ),
        'total' : time
    };
};

function animateTimer(element){
    element.className = "turn";
    setTimeout(function(){
        element.className = "";
    }, 700);
}