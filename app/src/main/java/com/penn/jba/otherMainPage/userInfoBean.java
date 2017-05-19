package com.penn.jba.otherMainPage;

import java.util.List;

/**
 * Created by raighne on 5/19/17.
 */

public class userInfoBean {
    /**
     * code : 1
     * data : {"profile":{"id":"7","nickname":"izzzzzz","gender":2,"head":"1494563656025.0isU496-1024x1024.jpg","age":18,"birthday":918489600000,"banner":"1494566721428.0BRMG1W-1024x1024.jpg","tags":"","photos":["1494563656025.0isU496-1024x1024.jpg","1494475848338.0YoLB90-1024x1024.jpg"]},"stats":{"fans":9,"momentBeLiked":10}}
     * msg : OK
     */

    private int code;
    private DataBean data;
    private String msg;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public static class DataBean {
        /**
         * profile : {"id":"7","nickname":"izzzzzz","gender":2,"head":"1494563656025.0isU496-1024x1024.jpg","age":18,"birthday":918489600000,"banner":"1494566721428.0BRMG1W-1024x1024.jpg","tags":"","photos":["1494563656025.0isU496-1024x1024.jpg","1494475848338.0YoLB90-1024x1024.jpg"]}
         * stats : {"fans":9,"momentBeLiked":10}
         */

        private ProfileBean profile;
        private StatsBean stats;

        public ProfileBean getProfile() {
            return profile;
        }

        public void setProfile(ProfileBean profile) {
            this.profile = profile;
        }

        public StatsBean getStats() {
            return stats;
        }

        public void setStats(StatsBean stats) {
            this.stats = stats;
        }

        public static class ProfileBean {
            /**
             * id : 7
             * nickname : izzzzzz
             * gender : 2
             * head : 1494563656025.0isU496-1024x1024.jpg
             * age : 18
             * birthday : 918489600000
             * banner : 1494566721428.0BRMG1W-1024x1024.jpg
             * tags :
             * photos : ["1494563656025.0isU496-1024x1024.jpg","1494475848338.0YoLB90-1024x1024.jpg"]
             */

            private String id;
            private String nickname;
            private int gender;
            private String head;
            private int age;
            private long birthday;
            private String banner;
            private String tags;
            private List<String> photos;

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public String getNickname() {
                return nickname;
            }

            public void setNickname(String nickname) {
                this.nickname = nickname;
            }

            public int getGender() {
                return gender;
            }

            public void setGender(int gender) {
                this.gender = gender;
            }

            public String getHead() {
                return head;
            }

            public void setHead(String head) {
                this.head = head;
            }

            public int getAge() {
                return age;
            }

            public void setAge(int age) {
                this.age = age;
            }

            public long getBirthday() {
                return birthday;
            }

            public void setBirthday(long birthday) {
                this.birthday = birthday;
            }

            public String getBanner() {
                return banner;
            }

            public void setBanner(String banner) {
                this.banner = banner;
            }

            public String getTags() {
                return tags;
            }

            public void setTags(String tags) {
                this.tags = tags;
            }

            public List<String> getPhotos() {
                return photos;
            }

            public void setPhotos(List<String> photos) {
                this.photos = photos;
            }
        }

        public static class StatsBean {
            /**
             * fans : 9
             * momentBeLiked : 10
             */

            private int fans;
            private int momentBeLiked;

            public int getFans() {
                return fans;
            }

            public void setFans(int fans) {
                this.fans = fans;
            }

            public int getMomentBeLiked() {
                return momentBeLiked;
            }

            public void setMomentBeLiked(int momentBeLiked) {
                this.momentBeLiked = momentBeLiked;
            }
        }
    }
}
