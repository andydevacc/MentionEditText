## MentionEditText
MentionEditText adds some useful features for mention string(@xxxx), such as highlight, intelligent deletion, intelligent selection and '@' input detection, etc.  

<img src="https://github.com/luckyandyzhang/MentionEditText/blob/master/art/demo.gif" width="300">  

## Usage
Insert the following dependency to build.gradle file of your project.

```groovy
repositories {
    maven {
        url "https://jitpack.io"
    }
}

dependencies {
    compile 'com.android.support:appcompat-v7:24.2.1'
    compile 'com.github.luckyandyzhang:MentionEditText:1.0.0'
}	
```

Use MentionEditText like a normal EditText:

```xml
<io.github.luckyandyzhang.mentionedittext.MentionEditText
    android:id="@+id/editText"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

In Java code:

```java
MentionEditText editText = ((MentionEditText) findViewById(R.id.editText));
List<String> mentionList = editText.getMentionList(true); //get a list of mention string
editText.setMentionTextColor(Color.RED); //optional, set highlight color of mention string
editText.setPattern("@[\\u4e00-\\u9fa5\\w\\-]+"); //optional, set regularExpression
editText.setOnMentionInputListener(new MentionEditText.OnMentionInputListener() {
    @Override
    public void onMentionCharacterInput() {
        //call when '@' character is inserted into EditText
    }
});

```

## License

    Copyright 2016 Andy

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.



