package com.holagundi.archiman.company;
import android.os.ParcelFileDescriptor;
interface ICompanyFileRenderer {
    ParcelFileDescriptor render(in ParcelFileDescriptor input, String contentType, int pageIndex);
}
