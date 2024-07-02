
 var show1 = function() {
  $("#demoQ1").fadeToggle("slow");
};
var show2 = function() {
  $("#demoQ2").fadeToggle("slow");
};
var show3 = function() {
  $("#demoQ3").fadeToggle("slow");
};
var show4 = function() {
  $("#demoQ4").fadeToggle("slow");
};
var hide1=function(){
$("#demoQ1").hide("slow");
};
 var hide2=function(){
$("#demoQ2").hide("slow");
};
var hide3=function(){
$("#demoQ3").hide("slow");
};
var hide4=function(){
$("#demoQ4").hide("slow");
};
var callbacks = $.Callbacks();
$(function(){

  $("#head1").click(function(){  
callbacks.add( show1 );
   callbacks.add( hide2 );
  callbacks.add( hide3 );
  callbacks.add( hide4 );
  callbacks.fire();
  callbacks.empty();
 
  });
  $("#head2").click(function(){
  callbacks.add( show2 );
   callbacks.add( hide1 );
  callbacks.add( hide3 );
  callbacks.add( hide4 );
  callbacks.fire();
  callbacks.empty();
  });
   $("#head3").click(function(){
  callbacks.add( show3 );
   callbacks.add( hide1 );
  callbacks.add( hide2 );
  callbacks.add( hide4 );
  callbacks.fire();
  callbacks.empty();
  });
  $("#head4").click(function(){
   callbacks.add( show4 );
   callbacks.add( hide1 );
  callbacks.add( hide2 );
  callbacks.add( hide3 );
  callbacks.fire();
  callbacks.empty();
  });
  $("#Q1").click(function(){
  $("#A1").fadeToggle();
  $("#right1").toggleClass("hidden");
  $("#down1").toggleClass("hidden");
     });
    $("#Q2").click(function(){
  $("#A2").fadeToggle();
  $("#right2").toggleClass("hidden");
  $("#down2").toggleClass("hidden");
     });
    $("#Q3").click(function(){
  $("#A3").fadeToggle();
  $("#right3").toggleClass("hidden");
  $("#down3").toggleClass("hidden");
    });
    $("#Q4").click(function(){
  $("#A4").fadeToggle();
  $("#right4").toggleClass("hidden");
  $("#down4").toggleClass("hidden");
     });
    $("#Q5").click(function(){
  $("#A5").fadeToggle();
  $("#right5").toggleClass("hidden");
  $("#down5").toggleClass("hidden");
     });
    $("#Q6").click(function(){
  $("#A6").fadeToggle("slow");
  $("#right6").toggleClass("hidden");
  $("#down6").toggleClass("hidden");
     });
    $("#Q7").click(function(){
  $("#A7").fadeToggle();
  $("#right7").toggleClass("hidden");
  $("#down7").toggleClass("hidden");
     });
    $("#Q8").click(function(){
  $("#A8").fadeToggle();
  $("#right8").toggleClass("hidden");
  $("#down8").toggleClass("hidden");
     });
   $("#Q9").click(function(){
  $("#A9").fadeToggle();
  $("#right9").toggleClass("hidden");
  $("#down9").toggleClass("hidden");
     });
    $("#Q10").click(function(){
  $("#A10").fadeToggle();
  $("#right10").toggleClass("hidden");
  $("#down10").toggleClass("hidden");
     });
    $("#Q11").click(function(){
  $("#A11").fadeToggle();
  $("#right11").toggleClass("hidden");
  $("#down11").toggleClass("hidden");
     });
     $("#Q12").click(function(){
  $("#A12").fadeToggle();
  $("#right12").toggleClass("hidden");
  $("#down12").toggleClass("hidden");
    });
	$("#Q13").click(function(){
  $("#A13").fadeToggle();
  $("#right13").toggleClass("hidden");
  $("#down13").toggleClass("hidden");
    });
	$("#Q14").click(function(){
  $("#A14").fadeToggle();
  $("#right14").toggleClass("hidden");
  $("#down14").toggleClass("hidden");
    });
});