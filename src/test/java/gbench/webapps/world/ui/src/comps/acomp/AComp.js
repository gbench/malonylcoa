import { mapGetters } from "vuex";
import { http_post, sqlquery, sqlquery2, sqlexecute } from "../../gbench/util/sqlquery";

const AComp = {

    template: `<div class="highlight">{{name}}</div>`,

    /**
     * 
     * @returns 
     */
    data() {
        return {label:"-"};
    },

    /**
     * 
     */
    mounted() {
        console.log(this.name);
        http_post("/h5/api/hello").then(res=>{
            this.label = res.data.message+" @ "+res.data.time;
            console.log(this.label);
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