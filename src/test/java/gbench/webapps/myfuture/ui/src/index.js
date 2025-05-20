import { createApp } from 'vue';
import { createRouter, createWebHashHistory } from 'vue-router';
import { AComp } from './comps/acomp/AComp';
import { BComp } from './comps/bcomp/BComp';
import { TraderComp } from './comps/tradercomp/TraderComp';
import { BrokerComp } from './comps/brokercomp/BrokerComp';
import { XchgComp } from './comps/xchgcomp/XchgComp';
import { CCPComp } from './comps/ccpcomp/CCPComp';
import { BankComp } from './comps/bankcomp/BankComp';
import { DataTable } from './ctrls/DataTable';
import { AutoComplete } from './ctrls/AutoComplete';
import { store } from './store';
import $ from 'jquery';

// 引入式样，不引入式样无法加载css
import "./css/index.css";

/**
 * 
 */
$(function () {
    /**
     * 加载页面模板，开始进行页面渲染。
     */
    function on_loaded(a_tpl, b_tpl) {

        // 添加模板
        if (a_tpl) AComp.template = a_tpl;
        if (b_tpl) BComp.template = b_tpl;

        // 设置路由
        const routes = [ // 定义控件路由
            { path: "/", redirect: "/AComp" }, // 设置默认路由
            { path: "/AComp", component: AComp }, // 
            { path: "/BComp", component: BComp }, // 
			{ path: "/TraderComp", component: TraderComp }, // 
			{ path: "/BrokerComp", component: BrokerComp }, // 
			{ path: "/XchgComp", component: XchgComp }, // 
			{ path: "/CCPComp", component: CCPComp }, // 
			{ path: "/BankComp", component: BankComp }, // 
        ];

        // 创建路由
        const router = createRouter({
            history: createWebHashHistory(),
            routes, // (缩写) 相当于 routes: routes
        }); // 路由对象

        // 生成vue对象
        const app = createApp({});
        app.component('data-table', DataTable);
        app.component('autocomplete', AutoComplete);
        app
            .use(store)
            .use(router)
            .mount('#app');
    }

    // 提取模板
    $.when( // 加载模板
        $.get("templates/a.html"), $.get("templates/b.html")
    ).done((a_tpl, b_tpl) => { // 模板加载
        on_loaded(a_tpl[0], b_tpl[0]);
    }).catch(e => { // 加载失败
        on_loaded();
    }); // when

});// ready function