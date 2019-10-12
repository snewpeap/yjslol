let href = window.location.href;
let loc = "#" + href.split("/")[4].split(".")[0];
let li = document.querySelector(loc);
li.classList.add("active");
