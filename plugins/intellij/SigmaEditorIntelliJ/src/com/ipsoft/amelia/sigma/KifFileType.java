package com.ipsoft.amelia.sigma;/*
Copyright 2014-2015 IPsoft

Author: Peigen You Peigen.You@ipsoft.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program ; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston,
MA  02111-1307 USA
*/

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class KifFileType extends LanguageFileType{
    public static final KifFileType INSTANCE=new KifFileType();

    private KifFileType(){
        super(KIFLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {

        return "Kif File";
    }

    @NotNull
    @Override
    public String getDescription() {

        return "Kif File";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {

        return "kif";
    }

    @Nullable
    @Override
    public Icon getIcon() {

        return SigmaIcon.FILE;
    }
}
