var host = window.location.href.replace("/index","");
// var host = "http://localhost:8087";
$(function () {
    // alert("hello2");
    add();
    writeBtnHandle();
    search();
});

function writeBtnHandle() {
    var writebtn = $("#writebtn");
    writebtn.click(function () {
        var adddiv = $("#adddiv");
        if (adddiv.css("display") == "none") {
            adddiv.css("display", "block");
        } else {
            adddiv.css("display", "none");
        }
    });
}

function add() {
    var addBtn = $("#addbtn");
    addBtn.click(function () {
        var addText = $("#addarea").val();
        if (addText != undefined && addText != '') {
            // addText=addText.replace("\n","\r\n");
            $.ajax(
                {
                    url: host + "/cfind/add",
                    type: "POST",
                    contentType:"application/json",
                    data: addText,
                    success: function (result) {
                        alert("添加成功！");
                    }
                });
        }
    });
}

function search() {
    var queryBtn = $("#querybtn");
    queryBtn.click(function () {
        $("#resdiv").html('');
        var queryText = $("#kwinput").val();
        if (queryText != undefined && queryText != '') {
            $.ajax(
                {
                    url: host + "/cfind/search/" + queryText,
                    type: "GET",
                    success: function (result) {
                        $("#resdiv").html(result);
                    }
                });
        }

    })
}

