/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.server.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import ratpack.server.ReloadInformant;
import ratpack.util.internal.IoUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static ratpack.util.ExceptionUtils.uncheck;

public class FileBackedReloadInformant implements ReloadInformant {
  private final Path file;
  private final boolean reloadable;
  private final Lock lock = new ReentrantLock();
  private final AtomicReference<FileTime> lastModifiedHolder = new AtomicReference<>(null);
  private final AtomicReference<ByteBuf> contentHolder = new AtomicReference<>();

  public FileBackedReloadInformant(Path file, boolean reloadable) {
    this.file = file;
    this.reloadable = reloadable;
  }

  @Override
  public boolean shouldReload() {
    if (!reloadable) {
      return false;
    }

    // If the file disappeared, wait a little for it to appear
    int i = 10;
    while (!Files.exists(file) && --i > 0) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        throw uncheck(e);
      }
    }

    if (!Files.exists(file)) {
      return false;
    }

    try {
      if (refreshNeeded()) {
        return refresh();
      }
    } catch (Exception e) {
      throw uncheck(e);
    }

    return false;
  }

  private boolean refreshNeeded() throws IOException {
    return !Files.getLastModifiedTime(file).equals(lastModifiedHolder.get()) || !isBytesAreSame();
  }

  private boolean isBytesAreSame() throws IOException {
    lock.lock();
    try {
      ByteBuf existing = contentHolder.get();
      //noinspection SimplifiableIfStatement
      if (existing == null) {
        return false;
      }

      return IoUtils.read(UnpooledByteBufAllocator.DEFAULT, file).equals(existing);
    } finally {
      lock.unlock();
    }
  }

  private boolean refresh() throws Exception {
    lock.lock();
    try {
      FileTime lastModifiedTime = Files.getLastModifiedTime(file);
      ByteBuf bytes = IoUtils.read(UnpooledByteBufAllocator.DEFAULT, file);

      if (lastModifiedTime.equals(lastModifiedHolder.get()) && bytes.equals(contentHolder.get())) {
        return false;
      }

      this.lastModifiedHolder.set(lastModifiedTime);
      this.contentHolder.set(bytes);
      return true;
    } finally {
      lock.unlock();
    }
  }
}