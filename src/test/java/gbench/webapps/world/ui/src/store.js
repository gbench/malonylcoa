import { ACompStore } from "./comps/acomp/ACompStore";
import { BCompStore } from "./comps/bcomp/BCompStore";

import { Store, createStore } from "vuex";

/**
 * 数据存储：数据库的本地缓存。
 */
const store = createStore({// 数据持久化层

    /**
     * 数据模块
     */
    modules: {
        ACompStore,
        BCompStore,
    },

    state: {// 本地缓存数据
    }, // state

    /**
    * 数据操作的公布方法：actions 只有通过 mutations 才能够修改 store的本地缓存的state 数据。
    */
    mutations: {// DAO

    },

    /**
    * 数据操作的异步方法
    */
    actions: {// Service

    },// actions

    getters: {// 数据的共享读取方法

    }
});

export { store };