let items = document.querySelectorAll('.nav li');

// 使用Array.prototype.forEach.call进行遍历
[].forEach.call(items, function (item) {
    //添加click事件
    item.addEventListener("click", function() {
        //遍历所有兄弟节点this.parentNode.children
        Array.prototype.forEach.call(this.parentNode.children, function (child) {
            //删除元素的某个class
            child.classList.remove("active");
        });
        this.classList.add("active");
        window.location.href = "/yjslol/frontend/" + item.innerText.toLowerCase() + ".html";
    });
});

startStreaming(
    function (res) {
        if(res === false){
            console.log('已经启动了！');
        }else{
            console.log('启动成功！');
        }
    },
    function (res) {
        console.log('启动失败！');
        console.log(res);
    }
);