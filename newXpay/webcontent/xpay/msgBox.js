 
function msgbox(title,content,focus,confir){
        create_mask();
        var temp="<div style=\"width:270px;border: 1px solid #CCC; font-weight: bold;font-size: 12px;\">"
                +"<table  cellspacing=\"0\" border=\"0\" style=\"width:270px\" bgcolor=\"white\">" 
                +"<tr><th align=\"center\" style=\"padding-top:7px;padding-bottom:7px\">"+title+"</th></tr>"
                +"<tr>"
                +"<td ><div style=\"background-color: #fff; font-weight: bold;font-size: 13px;padding:10px 20px ; text-align:left;\">"+content
                +"</div></td></tr></table>"
                +"<div style=\"text-align:center; padding:0px 0px 20px;background-color: #fff;\"><input type='button'  style=\"border:0px solid #999; background-color:white; width:75px; font-size:3; height:25px;\" value='确定支付' id=\"msgconfirmb\"   onclick=\"aa();\">"
                +"&nbsp;&nbsp;&nbsp;" 
                +"<input type='button' style=\"border:0px solid #999; background-color:white; width:50px;font-size:3; height:25px;\" value='取消'  id=\"msgcancelb\"   onClick='remove()'>"
                +"</div></div>";
        create_msgbox(380,200,temp)         
    }


    function get_width(){
        return (document.body.clientWidth+document.body.scrollLeft);
    }
    function get_height(){
        return (document.body.clientHeight+document.body.scrollTop);
    }
    function get_left(w){
        var bw=document.body.clientWidth;
        var bh=document.body.clientHeight;
        w=parseFloat(w);
        return (bw/2-w/2+document.body.scrollLeft);
    }
    function get_top(h){
        var bw=document.body.clientWidth;
        var bh=document.body.clientHeight;
        h=parseFloat(h);
        return (bh/2-h/2+document.body.scrollTop);
    }
    function create_mask(){//创建遮罩层的函数
        var mask=document.createElement("div");
        mask.id="mask";
        mask.style.position="absolute";
        mask.style.filter="progid:DXImageTransform.Microsoft.Alpha(style=4,opacity=25)";//IE的不透明设置
        mask.style.opacity=0.4;//Mozilla的不透明设置
        mask.style.background="black";
        mask.style.top="0px";
        mask.style.left="0px";
        mask.style.margin="80px auto 0";
        mask.style.width=get_width();
        mask.style.height=get_height();
        mask.style.zIndex=1000;
        document.body.appendChild(mask);
    }
    function create_msgbox(w,h,t){//创建弹出对话框的函数
        var box=document.createElement("div");
        box.id="msgbox";
        box.innerHTML=t;
        document.body.appendChild(box);
        document.getElementById("msgbox").style.cssText="width:100%;height:100%;position:fixed;top:0;left:0;z-index:2;background-color:rgba(0, 0, 0, .5);display:flex;flex-direction:column;align-items:center;padding-top:150px;"
//        re_pos();
    }
    function re_mask(){
        /*
        更改遮罩层的大小,确保在滚动以及窗口大小改变时还可以覆盖所有的内容
        */
        var mask=document.getElementById("mask")    ;
        if(null==mask)return;
        mask.style.width=get_width()+"px";
        mask.style.height=get_height()+"px";
    }
    function re_pos(){
        /*
        更改弹出对话框层的位置,确保在滚动以及窗口大小改变时一直保持在网页的最中间
        */
        var box=document.getElementById("msgbox");
        if(null!=box){
            var w=box.style.width;
            var h=box.style.height;
            box.style.left=get_left(w)+"px";
            box.style.top=get_top(h)+"px";
        }
    }
    function remove(){
        /*
        清除遮罩层以及弹出的对话框
        */
        var mask=document.getElementById("mask");
        var msgbox=document.getElementById("msgbox");
        if(null==mask&&null==msgbox)return;
        document.body.removeChild(mask);
        document.body.removeChild(msgbox);
    }
    
    function re_show(){
        /*
        重新显示遮罩层以及弹出窗口元素
        */
        re_pos();
        re_mask();    
    }
    function load_func(){
        /*
        加载函数,覆盖window的onresize和onscroll函数
        */
        window.onresize=re_show;
        window.onscroll=re_show;    
    }