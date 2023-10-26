import { mapGetters } from "vuex";
import { http_post, sqlquery, sqlquery2, sqlexecute } from "../../gbench/util/sqlquery";

const AComp = {

    template: `<div class="highlight">{{name}}</div>`,

    /**
     * 
     * @returns 
     */
    data() {
        return {label:"-",version:"-"};
    },

    /**
     * 
     */
    mounted() {
        console.log(this.name);
        
        // 开始信息
        http_post("/h5/api/hello").then(res=>{
            this.label = res.data.message+" @ "+res.data.time;
        });
        
        // 版本信息
        sqlquery("SELECT H2VERSION() VERSION FROM DUAL").then(res=>{
            this.version = res.data.data[0]["VERSION"];
        });
    },

    /**
     * 
     */
    computed: {
        /**
         * Getters 数据
         */
        ...mapGetters("ACompStore", ["name"]),
    }

};

export { AComp };