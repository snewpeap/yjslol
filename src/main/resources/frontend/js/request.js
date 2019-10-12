function updateStreaming(onError) {
    let a = [];
    $.ajax({
        type:'GET',
        url:'http://localhost:8196/streaming/champion/current',
        async:false,
        success:function (res) {
            a = res;
        },
        error:onError
    });
    return a;
}

function startStreaming(onSuccess,onError) {
    $.ajax({
        type:'POST',
        url:'http://localhost:8196/streaming/start',
        async:true,
        success:onSuccess,
        error:onError
    })
}