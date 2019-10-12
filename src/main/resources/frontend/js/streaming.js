let heros;
$.getJSON("hero.json",function (hero) {
    heros = hero;
});

let href = window.location.href;
let loc = "#" + href.split("/")[4].split(".")[0];
let li = document.querySelector(loc);
li.classList.add("active");


let positions = document.querySelectorAll('#position-group li');
[].forEach.call(positions, function (position) {
    position.addEventListener("click", function () {
        Array.prototype.forEach.call(this.parentNode.children, function (child) {
            child.classList.remove("active");
        });
        this.classList.add("active");
        positionNow = positionDict1[this.innerText];
        drawStreamingGraph();
    });
});

let viewPos = document.querySelectorAll('#day-group li');
[].forEach.call(viewPos, function (view) {
    view.addEventListener("click", function () {
        Array.prototype.forEach.call(this.parentNode.children, function (child) {
            child.classList.remove("active");
        });
        this.classList.add("active");
        dayOrXun = viewPosDict[this.innerText];
        drawStreamingGraph();
    });
});

let lastTimeStamp = 0;
let positionDict1 = {"TOP":0,"JUG":1,"MID":2,"ADC":3,"SUP":4};
let positionDict2 = {"上单":0,"打野":1,"中单":2,"下路":3,"辅助":4};
let viewPosDict = {"日":0,"旬":1};
let positionNow = 0;
let dayOrXun = 0;
let tenDaysRecord = [];
let day = 0;
let xunDaysRecord = [];

setInterval(getData,300);

function getData(){
    let stream = updateStreaming(
        function (res) {
            console.log('失败了！');
            stream = res;
        }
    );

    if(stream.timestamp !== lastTimeStamp){
        lastTimeStamp = stream.timestamp;
        let data = stream.map;
        let division = [[],[],[],[],[]];
        for(let ind = 0;ind<data.length;ind++){
            division[positionDict2[data[ind].pos]].push(data[ind]);
        }
        division.push(new Date(stream.timestamp * 1000).toLocaleString().split(" ")[0]);
        tenDaysRecord.push(division);
        day += 1;
        if(day === 10){
            day = 0;
        }

        if(tenDaysRecord.length === 11){
            tenDaysRecord.splice(0,1);
        }

        drawStreamingGraph();
    }

}

function drawStreamingGraph(){
    let nameGroup = [];
    let heroNameHtml = "<li><span>Top Rank</span></li>";
    let dayLength = tenDaysRecord.length;
    for(let topTen = 0;topTen<10;topTen++){
        let name = tenDaysRecord[dayLength-1][positionNow][topTen].cname;
        nameGroup.push(name);
        heroNameHtml += "<li><span>NO."+(topTen+1)+"</span>";
        for (let id = 0;id<145;id++){
            if(heros[id].name === name){
                heroNameHtml += "<img src='https://game.gtimg.cn/images/lol/act/img/champion/"+heros[id].alias+".png' class='heroIcon'></li>";
            }
        }
    }
    $("#top-ten-hero").html(heroNameHtml);

    let time = [];
    for(let day = dayLength-1;day>=0;day--){
        time.push(tenDaysRecord[day][5]);
    }
    for (let n = 0;n<10-tenDaysRecord.length;n++){
        time.push("未获取");
    }

    let mySeries = [];
    for(let name in nameGroup){
        let singleLineData = [];
        for(let day = dayLength-1;day>=0;day--){
            let dayData = tenDaysRecord[day][positionNow];
            for (let index1 = 0;index1<dayData.length;index1++){
                if(dayData[index1].cname === nameGroup[name]){
                    singleLineData.push(nameGroup[index1]);
                    break;
                }
            }
        }
        for (let n = 0;n<10-tenDaysRecord.length;n++){
            singleLineData.push("-1");
        }
        mySeries.push({
            name:nameGroup[name],
            type:'line',
            data:singleLineData
        });
    }

    nameGroup.push("");
    let option = {
        tooltip : {
            trigger: 'axis',
            axisPointer : {
                type: 'line'
            },
            formatter:  function (params) {
                let res='<div><p>时间：'+params[0].name+'</p></div>';
                return res;
            }
        },
        grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            containLabel: true
        },
        xAxis : [
            {
                type : 'category',
                data : time,
                axisTick: {
                    alignWithLabel: true
                }
            }
        ],
        yAxis : [
            {
                type: 'category',
                data: nameGroup.reverse(),
                boundaryGap: false
            },
        ],
        series : mySeries
    };

    let graph = echarts.init(document.getElementById("streaming-graph"));
    graph.setOption(option);
}