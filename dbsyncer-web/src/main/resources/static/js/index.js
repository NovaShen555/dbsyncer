// ******************* 初始化 *****************************
// 默认绑定菜单事件
$(function () {

    // 初始化版权信息
    $.getJSON("/config/system.json", function (data) {
        // 获取头部版权信息
        $("#logoName").html(data.headName);
        // 获取底部版权信息
        $("#copyrightInfo").html(data.footerName);
    });

    // 注销
    $("#nav_logout").click(function () {
        // 确认框确认是否注销
        BootstrapDialog.show({
            title: "提示",
            type: BootstrapDialog.TYPE_INFO,
            message: "确认注销帐号？",
            size: BootstrapDialog.SIZE_NORMAL,
            buttons: [{
                label: "确定",
                action: function (dialog) {
                    doPoster("/logout", null, function (data) {
                        location.href = $basePath;
                    });
                    dialog.close();
                }
            }, {
                label: "取消",
                action: function (dialog) {
                    dialog.close();
                }
            }]
        });
    });

    // 绑定所有的菜单链接点击事件，根据不同的URL加载页面
    $("#menu li a[url]").click(function () {
        // 加载页面
        $initContainer.load($(this).attr("url"));
    });

    // 头部导航栏选中切换事件
    var $menu = $('#menu > li');
    $menu.click(function () {
        $menu.removeClass('active');
        $(this).addClass('active');
    });

    // 显示主页
    backIndexPage();
});