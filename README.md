
# Data Saving - Bamboo Engine

![img0](https://postfiles.pstatic.net/MjAxOTA1MTFfMjUy/MDAxNTU3NTQzMjg4NjQy.zfCi_wEfQ1aRGygMFQnfCmTXH1cdL7gJc26TkHib7Lsg.SGwZEadAQwAXIHR3LhI8H5ZBTsca226i_WZVYx3Z0-8g.JPEG.tlaabs/bambu.jpg?type=w773)

#### 페이지를 로드할 때 이미지 다운로드를 막아서 데이터를 절약하는 기능입니다.
#### 사용자가 LongPress를 통해 보고싶은 이미지를 개별적으로 다운로드할 수 있습니다.

![img1](https://blogfiles.pstatic.net/MjAxOTA1MTFfMzYg/MDAxNTU3NTQ2NDYyMTAw.dh5lkRF4n6Owt9haozs8dAzn005OgPKCOyT0Imxr4lMg.u0d8f89NPYPZYG-o6caAsDg7PugRTQ_8jUB2nebIIxcg.GIF.tlaabs/sample_(1).gif)



## 작동 원리
1. HTML 파일 다운로드.
2. href,src 속성의 상대경로를 절대경로로 변환.
3. img 태그의 href 속성 값에 Bambu prefix를 붙여서 태그를 비활성화.
* Bambu prefix? : "bambu://[id]" 형식의 prefix 문자열.
4. HashMap에 <id,이미지 절대경로> 형식으로 저장.
5. 사용자가 longPress시 해당 Bambu prefix를 분석하여 id를 추출.
6. HashMap에서 id로 이미지 절대경로 가져오기.
7. 사용자가 선택한 img의 href 속성을 절대경로로 변환. 
8. 웹뷰 리로드

## 사용 라이브러리
##### [okhttp3](https://github.com/square/okhttp) - Http 통신 처리에 사용

##### [jsoup](https://jsoup.org/download) - HTML 태그 분석, 변환에 사용 
