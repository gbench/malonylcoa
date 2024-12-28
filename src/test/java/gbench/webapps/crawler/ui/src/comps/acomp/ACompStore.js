const ACompStore = {
    namespaced: true,

    /**
     * 
     * @returns 
     */
    state: () => {
        return { // 我的项目的页面数据-网页存储
            name: "AComp"
        };
    },

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

    /**
     * getters
     */
    getters: {
        /**
         * 提取 名称
         * 
         * @returns  name
         */
        name: state => {
            return state.name;
        },
    }

};

export { ACompStore };