# ModelessIME
モードレスIME

[![Workflow](https://github.com/kachaya/ModelessIME/actions/workflows/android.yml/badge.svg)](https://github.com/kachaya/ModelessIME/actions/workflows/android.yml)

## 概要
Android用のモードレスIMEを作るプロジェクトです。

「あa1」ボタンや「半角/全角」キーなどによる入力モード切り替えをおこなわずに、
英数字やかな漢字を入力するIMEです。

PalmのGraffiti入力のようなストローク入力が可能です。

ストローク入力キーボードからのシンボルキーボードへの切り替えは
左上から右下へのストロークを入力します。

## TODO
- [x] KeyboardViewを使わないSoftKeyboard
- [x] ローマ字かな変換
- [x] かな漢字変換
- [x] 学習辞書
- [x] 予測辞書
- [x] Graffiti入力
- [x] 表示速度改善
- [ ] 活用形対応、「見た」「来た」「勝った」など
- [x] シンボル入力
- [ ] 辞書ツール
- [ ] 後から変換


## ライセンス等
本ソフトウェアには Apache ライセンスが適用されます。
```
Copyright 2023 kachaya

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

本ソフトウェアでは jdbm-1.0 を使用しています。
```
/**
 * JDBM LICENSE v1.00
 *
 * Redistribution and use of this software and associated documentation
 * ("Software"), with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * 1. Redistributions of source code must retain copyright
 *    statements and notices.  Redistributions must also contain a
 *    copy of this document.
 *
 * 2. Redistributions in binary form must reproduce the
 *    above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other
 *    materials provided with the distribution.
 *
 * 3. The name "JDBM" must not be used to endorse or promote
 *    products derived from this Software without prior written
 *    permission of Cees de Groot.  For written permission,
 *    please contact cg@cdegroot.com.
 *
 * 4. Products derived from this Software may not be called "JDBM"
 *    nor may "JDBM" appear in their names without prior written
 *    permission of Cees de Groot. 
 *
 * 5. Due credit should be given to the JDBM Project
 *    (http://jdbm.sourceforge.net/).
 *
 * THIS SOFTWARE IS PROVIDED BY THE JDBM PROJECT AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 * CEES DE GROOT OR ANY CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Copyright 2000 (C) Cees de Groot. All Rights Reserved.
 * Contributions are Copyright (C) 2000 by their associated contributors.
 *
 * $Id: LICENSE.txt,v 1.1 2000/05/05 23:59:52 boisvert Exp $
 */

```

本ソフトウェアでは辞書データとして SudachiDict を使用しています。
```text
SudachiDict by Works Applications Co., Ltd. is licensed under the [Apache License, Version2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

   Copyright (c) 2017-2023 Works Applications Co., Ltd.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

This project includes UniDic and a part of NEologd.
```
SudachiDictの*_lex.csvファイルから本ソフトウェア用の辞書ファイルを生成するためのツールは[DicTool](https://github.com/kachaya/DicTool)で公開しています。
